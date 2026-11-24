package com.restaurante.application.service;

import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import java.util.Comparator;
import java.util.List;

/**
 * RF2 y RF3. Las reglas (orden abierta, línea PENDIENTE, cantidad...) viven en {@link Orden}.
 * Los platos y modificadores se consultan al servicio de Menú por el puerto {@link CatalogoMenu}.
 */
public class GestionarOrdenService implements GestionarOrdenUseCase {
    private final OrdenRepository ordenes;
    private final CatalogoMenu menu;

    public GestionarOrdenService(OrdenRepository ordenes, CatalogoMenu menu) {
        this.ordenes = ordenes;
        this.menu = menu;
    }

    @Override
    public Orden abrir(TipoOrden tipo, String referencia) {
        Orden orden = Orden.abrir(tipo, referencia);
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden agregarLinea(String ordenId, String platoId, int cantidad, List<String> modificadorIds) {
        Orden orden = buscarOrden(ordenId);
        Plato plato = menu.buscarPlato(platoId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe el plato " + platoId));

        orden.agregarLinea(plato, cantidad, buscarModificadores(modificadorIds));
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden modificarLinea(String ordenId, String lineaId, int cantidad, List<String> modificadorIds) {
        Orden orden = buscarOrden(ordenId);
        orden.modificarLinea(lineaId, cantidad, buscarModificadores(modificadorIds));
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden cancelarLinea(String ordenId, String lineaId) {
        Orden orden = buscarOrden(ordenId);
        orden.cancelarLinea(lineaId);
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden cancelarOrden(String ordenId) {
        Orden orden = buscarOrden(ordenId);
        orden.cancelar();
        ordenes.guardar(orden);
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
