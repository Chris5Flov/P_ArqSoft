package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.EstadoPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.PagosDePrueba;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.domain.Totales;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.ModificadorDeLinea;
import com.restaurante.eventos.DatosEventos.OrdenAbierta;
import com.restaurante.eventos.DatosEventos.OrdenPagada;
import com.restaurante.eventos.PublicadorEventosEnMemoria;
import com.restaurante.eventos.TiposEvento;
import com.restaurante.infrastructure.persistence.OrdenCajaRepositoryMemoria;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Caja con su propia copia de las órdenes, armada con los eventos de Órdenes. */
class CobrarOrdenServiceTest {

    /** Procesador falso que registra los montos cobrados y responde con el estado configurado. */
    private static class ProcesadorEspia implements ProcesadorPago {
        private final MetodoPago metodo;
        private EstadoPago respuesta = EstadoPago.APROBADO;
        final List<BigDecimal> cobros = new ArrayList<>();

        ProcesadorEspia(MetodoPago metodo) {
            this.metodo = metodo;
        }

        @Override
        public boolean soporta(MetodoPago m) {
            return m == metodo;
        }

        @Override
        public Pago cobrar(String ordenId, BigDecimal monto) {
            cobros.add(monto);
            return PagosDePrueba.pago(ordenId, respuesta, monto);
        }
    }

    private OrdenCajaRepositoryMemoria repositorio;
    private PublicadorEventosEnMemoria bus;
    private ProcesadorEspia efectivo;
    private ProcesadorEspia tarjeta;
    private CobrarOrdenService servicio;
    private Orden orden;

    @BeforeEach
    void setUp() {
        repositorio = new OrdenCajaRepositoryMemoria();
        bus = new PublicadorEventosEnMemoria();
        efectivo = new ProcesadorEspia(MetodoPago.EFECTIVO);
        tarjeta = new ProcesadorEspia(MetodoPago.TARJETA);
        servicio = new CobrarOrdenService(repositorio, List.of(efectivo, tarjeta), bus);

        // Caja arma su copia con lo que le llega de Órdenes por el bus.
        // 85 x 2 al 16 % + 20 x 1 sin impuesto = 190.00 + 27.20 de impuestos = 217.20
        servicio.ordenCreada(new OrdenAbierta("O1", "SALA", "Mesa 5", "2026-11-23T13:00:00"));
        servicio.lineaCreada(linea("L1", "P1", "Tacos", 2, "85.00", "0.16"));
        servicio.lineaCreada(linea("L2", "P2", "Agua", 1, "20.00", "0"));
        orden = repositorio.buscarPorId("O1").orElseThrow();
    }

    private static LineaDeOrden linea(String lineaId, String platoId, String plato, int cantidad,
                                      String precio, String tasa, ModificadorDeLinea... modificadores) {
        return new LineaDeOrden("O1", "Mesa 5", lineaId, platoId, plato, cantidad, precio, tasa,
                List.of(modificadores));
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }

    // ---------------------------------------------------------------- calcular totales (RF4)

    @Test
    void calculaLosTotalesConDescuentoYPropina() {
        // 190 - 19 = 171; impuestos de los tacos: 170 x 0.16 x 171 / 190 = 24.48
        Totales t = servicio.calcularTotales(orden.getId(), dinero("19"), dinero("10"));

        assertEquals(dinero("190.00"), t.subtotal());
        assertEquals(dinero("19.00"), t.descuento());
        assertEquals(dinero("24.48"), t.impuestos());
        assertEquals(dinero("10.00"), t.propina());
        assertEquals(dinero("205.48"), t.total());
    }

    @Test
    void descuentoYPropinaNulosSeTomanComoCero() {
        assertEquals(dinero("217.20"), servicio.calcularTotales(orden.getId(), null, null).total());
    }

    @Test
    void calcularRechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcularTotales("no-existe", BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void calcularRechazaDescuentoMayorAlSubtotalOPropinaNegativa() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcularTotales(orden.getId(), dinero("190.01"), BigDecimal.ZERO));
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcularTotales(orden.getId(), BigDecimal.ZERO, dinero("-1")));
    }

    // ---------------------------------------------------------------- cobrar (RF5)

    @Test
    void cobraConElProcesadorQueSoportaElMetodoYCierraLaOrden() {
        Pago pago = servicio.cobrar(orden.getId(), MetodoPago.TARJETA, BigDecimal.ZERO, dinero("10"));

        // total + propina
        assertEquals(dinero("227.20"), pago.monto());
        assertEquals(List.of(dinero("227.20")), tarjeta.cobros);
        assertTrue(efectivo.cobros.isEmpty());
        Orden guardada = repositorio.buscarPorId(orden.getId()).orElseThrow();
        assertEquals(EstadoOrden.CERRADA, guardada.getEstado());
        assertEquals(List.of(pago), guardada.getPagos());
        assertEquals(dinero("227.20"), guardada.getTotales().orElseThrow().total());
    }

    @Test
    void cobraElMismoMontoQueMuestranLosTotalesConDescuento() {
        Totales mostrados = servicio.calcularTotales(orden.getId(), dinero("19"), dinero("10"));

        Pago pago = servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, dinero("19"), dinero("10"));

        assertEquals(mostrados.total(), pago.monto());
        assertEquals(mostrados, orden.getTotales().orElseThrow());
    }

    @Test
    void unPagoRechazadoQuedaRegistradoYLaOrdenSigueAbierta() {
        tarjeta.respuesta = EstadoPago.RECHAZADO;

        Pago rechazado = servicio.cobrar(orden.getId(), MetodoPago.TARJETA, null, null);

        assertFalse(rechazado.esAprobado());
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
        assertEquals(List.of(rechazado), orden.getPagos());

        // Se puede reintentar con otro método
        Pago aprobado = servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, null, null);
        assertTrue(aprobado.esAprobado());
        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
        assertEquals(List.of(rechazado, aprobado), orden.getPagos());
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar("no-existe", MetodoPago.EFECTIVO, null, null));
    }

    @Test
    void rechazaUnMetodoDePagoNulo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), null, null, null));
    }

    @Test
    void rechazaUnMetodoSinProcesadorYNoCobraNada() {
        // Solo hay procesadores de efectivo y tarjeta; no existe uno para la pasarela digital.
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), MetodoPago.PASARELA_DIGITAL, null, null));

        assertTrue(efectivo.cobros.isEmpty());
        assertTrue(tarjeta.cobros.isEmpty());
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void rechazaUnaPropinaNegativaSinCobrar() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, null, dinero("-1")));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void noCobraUnaOrdenSinLineas() {
        servicio.ordenCreada(new OrdenAbierta("O2", "PARA_LLEVAR", "Pedido 1", null));
        Orden vacia = repositorio.buscarPorId("O2").orElseThrow();

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(vacia.getId(), MetodoPago.EFECTIVO, null, null));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void noCobraSiLaOrdenYaEstabaCerrada() {
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, null, null));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void funcionaConLosAdaptadoresDelProyecto() {
        CobrarOrdenService real = new CobrarOrdenService(repositorio,
                List.of(new PagoEfectivoAdapter(), new PagoTarjetaSimuladoAdapter()), bus);

        Pago pago = real.cobrar(orden.getId(), MetodoPago.TARJETA, null, null);

        assertEquals(EstadoPago.APROBADO, pago.estado());
        assertEquals(orden.getId(), pago.ordenId());
        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
    }

    // ---------------------------------------------------------------- la copia de la orden (eventos)

    @Test
    void laCopiaConservaElIdLaFechaYLosPreciosDeLaOrdenOriginal() {
        assertEquals("O1", orden.getId());
        assertEquals(TipoOrden.SALA, orden.getTipo());
        assertEquals("Mesa 5", orden.getReferencia());
        assertEquals(LocalDateTime.of(2026, 11, 23, 13, 0), orden.getFechaCreacion());
        assertEquals(List.of("L1", "L2"), orden.getLineas().stream().map(l -> l.getId()).toList());
    }

    @Test
    void usaElCostoDeLosModificadoresQueVieneEnElEvento() {
        servicio.lineaCreada(linea("L3", "P1", "Tacos", 1, "85.00", "0.16",
                new ModificadorDeLinea("M1", "Queso extra", "EXTRA", "15.00")));

        // 217.20 + 100 x 1.16 = 333.20
        assertEquals(dinero("333.20"), servicio.calcularTotales("O1", null, null).total());
    }

    @Test
    void siUnEventoLlegaDosVecesNoSeDuplica() {
        servicio.ordenCreada(new OrdenAbierta("O1", "SALA", "Mesa 5", "2026-11-23T13:00:00"));
        servicio.lineaCreada(linea("L1", "P1", "Tacos", 2, "85.00", "0.16"));

        assertEquals(2, repositorio.buscarPorId("O1").orElseThrow().getLineas().size());
        assertEquals(dinero("217.20"), servicio.calcularTotales("O1", null, null).total());
    }

    @Test
    void modificarYCancelarLineasCambiaLosTotales() {
        servicio.lineaModificada(linea("L1", "P1", "Tacos", 1, "85.00", "0.16"));
        assertEquals(dinero("118.60"), servicio.calcularTotales("O1", null, null).total());

        servicio.lineaCancelada("O1", "L2");
        assertEquals(dinero("98.60"), servicio.calcularTotales("O1", null, null).total());
    }

    @Test
    void unaOrdenCanceladaYaNoSePuedeCobrar() {
        servicio.ordenCancelada("O1");

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar("O1", MetodoPago.EFECTIVO, null, null));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void publicaOrdenCobradaSoloCuandoElPagoSeAprueba() {
        tarjeta.respuesta = EstadoPago.RECHAZADO;
        servicio.cobrar("O1", MetodoPago.TARJETA, null, null);
        assertTrue(bus.publicados().isEmpty());

        Pago pago = servicio.cobrar("O1", MetodoPago.EFECTIVO, dinero("19"), dinero("10"));

        assertEquals(1, bus.publicados(TiposEvento.ORDEN_COBRADA).size());
        OrdenPagada datos = bus.publicados().get(0).datosComo(OrdenPagada.class);
        assertEquals("O1", datos.ordenId());
        assertEquals(pago.id(), datos.pagoId());
        assertEquals("EFECTIVO", datos.metodo());
        assertEquals("205.48", datos.monto());
        assertEquals("19.00", datos.descuento());
        assertEquals("10.00", datos.propina());
    }

    @Test
    void ignoraLosCambiosQueLleganDespuesDeCobrar() {
        servicio.cobrar("O1", MetodoPago.EFECTIVO, null, null);

        servicio.lineaCreada(linea("L9", "P2", "Agua", 1, "20.00", "0"));
        servicio.ordenCancelada("O1");

        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
        assertEquals(2, orden.getLineas().size());
    }

    @Test
    void ignoraLineasDeUnaOrdenQueNoConoce() {
        servicio.lineaCreada(new LineaDeOrden("otra", "Mesa 9", "L1", "P1", "Tacos", 1, "85.00", "0.16", List.of()));

        assertTrue(repositorio.buscarPorId("otra").isEmpty());
    }
}
