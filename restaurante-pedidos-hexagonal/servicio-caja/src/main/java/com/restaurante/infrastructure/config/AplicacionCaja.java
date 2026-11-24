package com.restaurante.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.RecibirOrdenesUseCase;
import com.restaurante.application.service.CobrarOrdenService;
import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.DatosEventos.OrdenAbierta;
import com.restaurante.eventos.DatosEventos.OrdenId;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.Suscriptor;
import com.restaurante.eventos.TiposEvento;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoPasarelaSimuladoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.infrastructure.persistence.OrdenCajaRepositoryMemoria;

/** Arma el servicio de Caja: su copia de las órdenes, los procesadores de pago y qué eventos escucha. */
public record AplicacionCaja(CobrarOrdenUseCase cobrarOrden, RecibirOrdenesUseCase recibirOrdenes) {

    public static AplicacionCaja crear(PublicadorEventos eventos) {
        CobrarOrdenService servicio = new CobrarOrdenService(
                new OrdenCajaRepositoryMemoria(),
                List.of(new PagoEfectivoAdapter(), new PagoTarjetaSimuladoAdapter(), new PagoPasarelaSimuladoAdapter()),
                eventos);
        return new AplicacionCaja(servicio, servicio);
    }

    /** Qué hace Caja con cada evento al que se suscribe. */
    public Map<String, Suscriptor> suscripciones() {
        Map<String, Suscriptor> suscripciones = new LinkedHashMap<>();
        suscripciones.put(TiposEvento.ORDEN_CREADA,
                e -> recibirOrdenes.ordenCreada(e.datosComo(OrdenAbierta.class)));
        suscripciones.put(TiposEvento.LINEA_CREADA,
                e -> recibirOrdenes.lineaCreada(e.datosComo(LineaDeOrden.class)));
        suscripciones.put(TiposEvento.LINEA_MODIFICADA,
                e -> recibirOrdenes.lineaModificada(e.datosComo(LineaDeOrden.class)));
        suscripciones.put(TiposEvento.LINEA_CANCELADA, e -> {
            LineaDeOrdenId id = e.datosComo(LineaDeOrdenId.class);
            recibirOrdenes.lineaCancelada(id.ordenId(), id.lineaId());
        });
        suscripciones.put(TiposEvento.ORDEN_CANCELADA,
                e -> recibirOrdenes.ordenCancelada(e.datosComo(OrdenId.class).ordenId()));
        return suscripciones;
    }
}
