package com.restaurante.application.service;

import java.util.List;

import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.in.RecibirPedidosUseCase;
import com.restaurante.application.port.out.LineaCocinaRepository;
import com.restaurante.domain.LineaCocina;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.DatosEventos.ModificadorDeLinea;
import com.restaurante.eventos.Evento;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.TiposEvento;

/**
 * RF3 del lado de Cocina. Trabaja con su propio repositorio de líneas, que se llena con los
 * eventos de Órdenes, y avisa con LineaIniciada y LineaLista cuando el cocinero avanza una línea.
 */
public class GestionarCocinaService implements GestionarCocinaUseCase, RecibirPedidosUseCase {

    private final LineaCocinaRepository lineas;
    private final PublicadorEventos eventos;

    public GestionarCocinaService(LineaCocinaRepository lineas, PublicadorEventos eventos) {
        this.lineas = lineas;
        this.eventos = eventos;
    }

    // ---------------------------------------------------------------- el cocinero

    @Override
    public List<LineaEnCocina> listarPendientes() {
        return lineas.listar().stream()
                .filter(LineaCocina::estaPorAtender)
                .map(l -> new LineaEnCocina(l.getOrdenId(), l.getLineaId(), l.getReferenciaOrden(),
                        l.getPlato(), l.getCantidad(), l.getModificadores(), l.getEstado()))
                .toList();
    }

    @Override
    public void iniciarCoccion(String ordenId, String lineaId) {
        LineaCocina linea = buscar(ordenId, lineaId);
        linea.iniciarCoccion();
        lineas.guardar(linea);
        eventos.publicar(Evento.nuevo(TiposEvento.LINEA_INICIADA, new LineaDeOrdenId(ordenId, lineaId)));
    }

    @Override
    public void marcarLista(String ordenId, String lineaId) {
        LineaCocina linea = buscar(ordenId, lineaId);
        linea.marcarLista();
        lineas.guardar(linea);
        eventos.publicar(Evento.nuevo(TiposEvento.LINEA_LISTA, new LineaDeOrdenId(ordenId, lineaId)));
    }

    // ---------------------------------------------------------------- eventos de Órdenes

    @Override
    public void lineaCreada(LineaDeOrden datos) {
        if (lineas.buscar(datos.ordenId(), datos.lineaId()).isPresent()) {
            return; // el bus pudo reintentar: el mismo evento dos veces no duplica la línea
        }
        lineas.guardar(new LineaCocina(datos.ordenId(), datos.lineaId(), datos.referenciaOrden(),
                datos.platoNombre(), datos.cantidad(), nombres(datos)));
    }

    @Override
    public void lineaModificada(LineaDeOrden datos) {
        lineas.buscar(datos.ordenId(), datos.lineaId()).ifPresentOrElse(
                linea -> aplicar(linea, () -> linea.actualizar(datos.cantidad(), nombres(datos))),
                () -> lineaCreada(datos));
    }

    @Override
    public void lineaCancelada(String ordenId, String lineaId) {
        lineas.buscar(ordenId, lineaId).ifPresent(linea -> aplicar(linea, linea::cancelar));
    }

    @Override
    public void ordenCancelada(String ordenId) {
        lineas.listarDeOrden(ordenId).stream()
                .filter(LineaCocina::estaPorAtender)
                .forEach(linea -> aplicar(linea, linea::cancelar));
    }

    @Override
    public void ordenCobrada(String ordenId) {
        lineas.listarDeOrden(ordenId).forEach(linea -> aplicar(linea, linea::cerrarPorCobro));
    }

    // ---------------------------------------------------------------- utilidades

    /**
     * Aplica un cambio que viene de Órdenes. Si llega cuando la cocina ya empezó la línea
     * (el mesero y el cocinero actuaron casi al mismo tiempo), manda lo que dice Cocina:
     * el cambio se ignora y se avisa en la consola.
     */
    private void aplicar(LineaCocina linea, Runnable cambio) {
        try {
            cambio.run();
            lineas.guardar(linea);
        } catch (ReglaDeNegocioException e) {
            System.err.println("Cocina ignoró un cambio de la orden " + linea.getOrdenId() + ": " + e.getMessage());
        }
    }

    private LineaCocina buscar(String ordenId, String lineaId) {
        return lineas.buscar(ordenId, lineaId).orElseThrow(() -> new ReglaDeNegocioException(
                "No existe la línea " + lineaId + " de la orden " + ordenId + " en cocina"));
    }

    private static List<String> nombres(LineaDeOrden datos) {
        return datos.modificadores() == null ? List.of()
                : datos.modificadores().stream().map(ModificadorDeLinea::nombre).toList();
    }
}
