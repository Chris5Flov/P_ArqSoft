package com.restaurante.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.in.RecibirPedidosUseCase;
import com.restaurante.application.service.GestionarCocinaService;
import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.DatosEventos.OrdenId;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.Suscriptor;
import com.restaurante.eventos.TiposEvento;
import com.restaurante.infrastructure.persistence.LineaCocinaRepositoryMemoria;

/** Arma el servicio de Cocina: su repositorio de líneas, el caso de uso y qué eventos escucha. */
public record AplicacionCocina(GestionarCocinaUseCase gestionarCocina, RecibirPedidosUseCase recibirPedidos) {

    public static AplicacionCocina crear(PublicadorEventos eventos) {
        GestionarCocinaService servicio = new GestionarCocinaService(new LineaCocinaRepositoryMemoria(), eventos);
        return new AplicacionCocina(servicio, servicio);
    }

    /** Qué hace Cocina con cada evento al que se suscribe. */
    public Map<String, Suscriptor> suscripciones() {
        Map<String, Suscriptor> suscripciones = new LinkedHashMap<>();
        suscripciones.put(TiposEvento.LINEA_CREADA,
                e -> recibirPedidos.lineaCreada(e.datosComo(LineaDeOrden.class)));
        suscripciones.put(TiposEvento.LINEA_MODIFICADA,
                e -> recibirPedidos.lineaModificada(e.datosComo(LineaDeOrden.class)));
        suscripciones.put(TiposEvento.LINEA_CANCELADA, e -> {
            LineaDeOrdenId id = e.datosComo(LineaDeOrdenId.class);
            recibirPedidos.lineaCancelada(id.ordenId(), id.lineaId());
        });
        suscripciones.put(TiposEvento.ORDEN_CANCELADA,
                e -> recibirPedidos.ordenCancelada(e.datosComo(OrdenId.class).ordenId()));
        return suscripciones;
    }
}
