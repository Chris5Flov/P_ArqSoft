package com.restaurante.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.RecibirOrdenesUseCase;
import com.restaurante.application.port.out.OrdenCajaRepository;
import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.domain.Totales;
import com.restaurante.eventos.DatosEventos;
import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.ModificadorDeLinea;
import com.restaurante.eventos.DatosEventos.OrdenAbierta;
import com.restaurante.eventos.Evento;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.TiposEvento;

/**
 * RF4 y RF5: calcula los totales y cobra la orden con el procesador que soporte el método elegido.
 * Si el pago se aprueba la orden se cierra y se publica OrdenCobrada; si se rechaza queda
 * registrado en Caja y la orden sigue abierta.
 *
 * Caja no lee las órdenes de Órdenes: trabaja con su propia copia, que arma con los eventos
 * OrdenCreada, LineaCreada, LineaModificada, LineaCancelada y OrdenCancelada. Los precios
 * vienen en el evento tal como se congelaron al pedir, así que los totales salen iguales.
 */
public class CobrarOrdenService implements CobrarOrdenUseCase, RecibirOrdenesUseCase {
    private final OrdenCajaRepository ordenes;
    private final List<ProcesadorPago> procesadores;
    private final PublicadorEventos eventos;

    public CobrarOrdenService(OrdenCajaRepository ordenes, List<ProcesadorPago> procesadores,
                              PublicadorEventos eventos) {
        this.ordenes = ordenes;
        this.procesadores = List.copyOf(procesadores);
        this.eventos = eventos;
    }

    // ---------------------------------------------------------------- el cajero (RF4 y RF5)

    @Override
    public Totales calcularTotales(String ordenId, BigDecimal descuento, BigDecimal propina) {
        return buscarOrden(ordenId).calcularTotales(descuento, propina);
    }

    @Override
    public List<Pago> listarPagos(String ordenId) {
        return buscarOrden(ordenId).getPagos();
    }

    @Override
    public Pago cobrar(String ordenId, MetodoPago metodo, BigDecimal descuento, BigDecimal propina) {
        if (metodo == null) {
            throw new ReglaDeNegocioException("El método de pago es obligatorio");
        }
        Orden orden = buscarOrden(ordenId);

        // Se valida todo antes de cobrar: si algo falla, no se hace ningún cargo.
        Totales totales = orden.totalesParaCobrar(descuento, propina);
        ProcesadorPago procesador = elegirProcesador(metodo);

        Pago pago = procesador.cobrar(orden.getId(), totales.total());
        orden.registrarPago(pago);
        if (pago.esAprobado()) {
            orden.cerrar(descuento, propina);
        }
        ordenes.guardar(orden);
        if (pago.esAprobado()) {
            eventos.publicar(Evento.nuevo(TiposEvento.ORDEN_COBRADA,
                    DatosEventos.ordenPagada(pago, descuento, propina)));
        }
        return pago;
    }

    // ---------------------------------------------------------------- eventos de Órdenes

    @Override
    public void ordenCreada(OrdenAbierta datos) {
        if (ordenes.buscarPorId(datos.ordenId()).isPresent()) {
            return; // el bus la mandó dos veces
        }
        ordenes.guardar(Orden.copia(datos.ordenId(), TipoOrden.valueOf(datos.tipo()), datos.referencia(),
                datos.fecha() == null ? null : LocalDateTime.parse(datos.fecha())));
    }

    @Override
    public void lineaCreada(LineaDeOrden datos) {
        actualizar(datos.ordenId(), orden -> {
            if (!orden.tieneLinea(datos.lineaId())) {
                orden.agregarLinea(datos.lineaId(), plato(datos), datos.cantidad(), modificadores(datos));
            }
        });
    }

    @Override
    public void lineaModificada(LineaDeOrden datos) {
        actualizar(datos.ordenId(),
                orden -> orden.modificarLinea(datos.lineaId(), datos.cantidad(), modificadores(datos)));
    }

    @Override
    public void lineaCancelada(String ordenId, String lineaId) {
        actualizar(ordenId, orden -> orden.cancelarLinea(lineaId));
    }

    @Override
    public void ordenCancelada(String ordenId) {
        actualizar(ordenId, Orden::cancelar);
    }

    // ---------------------------------------------------------------- utilidades

    /**
     * Aplica a la copia un cambio que viene de Órdenes. Si ya no aplica (por ejemplo, la orden
     * se cobró aquí mientras el mesero agregaba algo) se ignora y se avisa en la consola.
     */
    private void actualizar(String ordenId, Consumer<Orden> cambio) {
        try {
            Orden orden = buscarOrden(ordenId);
            cambio.accept(orden);
            ordenes.guardar(orden);
        } catch (ReglaDeNegocioException e) {
            System.err.println("Caja ignoró un cambio de la orden " + ordenId + ": " + e.getMessage());
        }
    }

    /** El plato con el precio y la tasa con los que se pidió, no los de ahora en el menú. */
    private static Plato plato(LineaDeOrden datos) {
        return new Plato(datos.platoId(), datos.platoNombre(), decimal(datos.precioUnitario()),
                decimal(datos.tasaImpuesto()));
    }

    private static List<Modificador> modificadores(LineaDeOrden datos) {
        if (datos.modificadores() == null) {
            return List.of();
        }
        return datos.modificadores().stream().map(CobrarOrdenService::modificador).toList();
    }

    private static Modificador modificador(ModificadorDeLinea m) {
        return new Modificador(m.id(), m.nombre(), TipoModificador.valueOf(m.tipo()), decimal(m.costoExtra()));
    }

    private static BigDecimal decimal(String valor) {
        return valor == null || valor.isBlank() ? BigDecimal.ZERO : new BigDecimal(valor);
    }

    private Orden buscarOrden(String ordenId) {
        return ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));
    }

    private ProcesadorPago elegirProcesador(MetodoPago metodo) {
        return procesadores.stream()
                .filter(p -> p.soporta(metodo))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No hay un procesador de pago configurado para el método " + metodo));
    }
}
