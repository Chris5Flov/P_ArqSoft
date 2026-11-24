package com.restaurante.application.service;

import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.in.RegistrarAvanceCocinaUseCase;
import com.restaurante.application.port.in.RegistrarCobroUseCase;
import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.EstadoPago;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.eventos.DatosEventos;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.DatosEventos.OrdenId;
import com.restaurante.eventos.DatosEventos.OrdenPagada;
import com.restaurante.eventos.Evento;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.TiposEvento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * RF2 y RF3. Las reglas (orden abierta, línea PENDIENTE, cantidad...) viven en {@link Orden}.
 * Los platos y modificadores se consultan al servicio de Menú por el puerto {@link CatalogoMenu}.
 * Cada cambio en las líneas se avisa por el bus (LineaCreada, LineaModificada, LineaCancelada,
 * OrdenCancelada) y el avance de cocina llega de vuelta como LineaIniciada y LineaLista.
 */
public class GestionarOrdenService
        implements GestionarOrdenUseCase, RegistrarAvanceCocinaUseCase, RegistrarCobroUseCase {
    private final OrdenRepository ordenes;
    private final CatalogoMenu menu;
    private final PublicadorEventos eventos;

    public GestionarOrdenService(OrdenRepository ordenes, CatalogoMenu menu, PublicadorEventos eventos) {
        this.ordenes = ordenes;
        this.menu = menu;
        this.eventos = eventos;
    }

    @Override
    public Orden abrir(TipoOrden tipo, String referencia) {
        Orden orden = Orden.abrir(tipo, referencia);
        ordenes.guardar(orden);
        eventos.publicar(Evento.nuevo(TiposEvento.ORDEN_CREADA, DatosEventos.ordenAbierta(orden)));
        return orden;
    }

    @Override
    public Orden agregarLinea(String ordenId, String platoId, int cantidad, List<String> modificadorIds) {
        Orden orden = buscarOrden(ordenId);
        Plato plato = menu.buscarPlato(platoId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe el plato " + platoId));

        LineaPedido linea = orden.agregarLinea(plato, cantidad, buscarModificadores(modificadorIds));
        ordenes.guardar(orden);
        eventos.publicar(Evento.nuevo(TiposEvento.LINEA_CREADA, DatosEventos.linea(orden, linea)));
        return orden;
    }

    @Override
    public Orden modificarLinea(String ordenId, String lineaId, int cantidad, List<String> modificadorIds) {
        Orden orden = buscarOrden(ordenId);
        orden.modificarLinea(lineaId, cantidad, buscarModificadores(modificadorIds));
        ordenes.guardar(orden);
        eventos.publicar(Evento.nuevo(TiposEvento.LINEA_MODIFICADA,
                DatosEventos.linea(orden, buscarLinea(orden, lineaId))));
        return orden;
    }

    @Override
    public Orden cancelarLinea(String ordenId, String lineaId) {
        Orden orden = buscarOrden(ordenId);
        orden.cancelarLinea(lineaId);
        ordenes.guardar(orden);
        eventos.publicar(Evento.nuevo(TiposEvento.LINEA_CANCELADA, new LineaDeOrdenId(ordenId, lineaId)));
        return orden;
    }

    @Override
    public Orden cancelarOrden(String ordenId) {
        Orden orden = buscarOrden(ordenId);
        orden.cancelar();
        ordenes.guardar(orden);
        eventos.publicar(Evento.nuevo(TiposEvento.ORDEN_CANCELADA, new OrdenId(ordenId)));
        return orden;
    }

    @Override
    public Orden consultar(String ordenId) {
        return buscarOrden(ordenId);
    }

    @Override
    public List<Orden> listar() {
        return ordenes.listar().stream()
                .sorted(Comparator.comparing(Orden::getFechaCreacion))
                .toList();
    }

    @Override
    public List<Orden> listarAbiertas() {
        return listar().stream()
                .filter(o -> o.getEstado() == EstadoOrden.ABIERTA)
                .toList();
    }

    @Override
    public List<Plato> consultarPlatos() {
        return menu.listarPlatos();
    }

    @Override
    public List<Modificador> consultarModificadores() {
        return menu.listarModificadores();
    }

    // ---------------------------------------------------------------- avance de cocina (eventos)

    @Override
    public void lineaIniciada(String ordenId, String lineaId) {
        actualizarDesdeCocina(ordenId, orden -> orden.iniciarCoccionLinea(lineaId));
    }

    @Override
    public void lineaLista(String ordenId, String lineaId) {
        actualizarDesdeCocina(ordenId, orden -> orden.marcarLineaLista(lineaId));
    }

    /**
     * Cocina manda sobre el estado de la línea. Si el aviso ya no aplica (por ejemplo, la orden
     * se canceló mientras tanto) se ignora y se avisa en la consola.
     */
    private void actualizarDesdeCocina(String ordenId, Consumer<Orden> cambio) {
        try {
            Orden orden = buscarOrden(ordenId);
            cambio.accept(orden);
            ordenes.guardar(orden);
        } catch (ReglaDeNegocioException e) {
            System.err.println("Órdenes ignoró un aviso de cocina para la orden " + ordenId + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- cobro en Caja (evento)

    /**
     * Caja ya cobró: se registra el mismo pago y la orden se cierra con el descuento y la
     * propina que se usaron allá. Si llega dos veces, la segunda no hace nada.
     */
    @Override
    public void ordenCobrada(OrdenPagada datos) {
        try {
            Orden orden = buscarOrden(datos.ordenId());
            if (orden.getEstado() == EstadoOrden.CERRADA) {
                return;
            }
            BigDecimal descuento = new BigDecimal(datos.descuento());
            BigDecimal propina = new BigDecimal(datos.propina());
            BigDecimal monto = new BigDecimal(datos.monto());
            // Se revisa antes de tocar la orden, para no dejarla con un pago y sin cerrar.
            BigDecimal total = orden.calcularTotales(descuento, propina).total();
            if (monto.compareTo(total) < 0) {
                throw new ReglaDeNegocioException("el pago de " + monto.toPlainString()
                        + " no cubre el total que tiene Órdenes (" + total.toPlainString() + ")");
            }
            orden.registrarPago(new Pago(datos.pagoId(), datos.ordenId(), LocalDateTime.parse(datos.fecha()),
                    EstadoPago.APROBADO, MetodoPago.valueOf(datos.metodo()), monto, datos.referenciaPago()));
            orden.cerrar(descuento, propina);
            ordenes.guardar(orden);
        } catch (ReglaDeNegocioException e) {
            System.err.println("Órdenes no pudo cerrar la orden " + datos.ordenId()
                    + " después del cobro en Caja: " + e.getMessage());
        }
    }

    private static LineaPedido buscarLinea(Orden orden, String lineaId) {
        return orden.getLineas().stream().filter(l -> l.getId().equals(lineaId)).findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la línea " + lineaId));
    }

    private Orden buscarOrden(String ordenId) {
        return ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));
    }

    private List<Modificador> buscarModificadores(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .map(id -> menu.buscarModificador(id)
                        .orElseThrow(() -> new ReglaDeNegocioException("No existe el modificador " + id)))
                .toList();
    }
}
