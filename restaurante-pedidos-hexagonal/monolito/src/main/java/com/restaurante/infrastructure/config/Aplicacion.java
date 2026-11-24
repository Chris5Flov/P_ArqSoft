package com.restaurante.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.in.RegistrarAvanceCocinaUseCase;
import com.restaurante.application.port.in.RegistrarCobroUseCase;
import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.service.GestionarOrdenService;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.DatosEventos.OrdenPagada;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.Suscriptor;
import com.restaurante.eventos.TiposEvento;
import com.restaurante.infrastructure.client.MenuRestClient;
import com.restaurante.infrastructure.eventos.ClienteBusEventos;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;

/**
 * Raíz de composición de Órdenes: conecta los adaptadores de salida con los casos de uso.
 * La comparten la consola y la web.
 *
 * El menú se consulta por REST al servicio de Menú (CatalogoMenu). Cocina y Caja son otros
 * servicios: se enteran de las órdenes por el bus de eventos y avisan de vuelta cuando una
 * línea avanza (LineaIniciada, LineaLista) o cuando la orden se paga (OrdenCobrada).
 */
public record Aplicacion(GestionarOrdenUseCase gestionarOrden,
                         RegistrarAvanceCocinaUseCase avanceCocina,
                         RegistrarCobroUseCase registrarCobro) {

    public static final String URL_MENU_POR_DEFECTO = "http://localhost:8081";

    public static Aplicacion crear(String urlMenu, String urlBus) {
        return crear(new MenuRestClient(urlMenu), new ClienteBusEventos(urlBus));
    }

    public static Aplicacion crear(CatalogoMenu catalogoMenu, PublicadorEventos eventos) {
        // 1. Adaptador de salida propio: la persistencia en memoria
        OrdenRepository ordenRepository = new OrdenRepositoryMemoria();

        // 2. Caso de uso (recibe los puertos de salida)
        GestionarOrdenService ordenes = new GestionarOrdenService(ordenRepository, catalogoMenu, eventos);
        return new Aplicacion(ordenes, ordenes, ordenes);
    }

    /** Qué hace Órdenes con cada evento al que se suscribe. */
    public Map<String, Suscriptor> suscripciones() {
        Map<String, Suscriptor> suscripciones = new LinkedHashMap<>();
        suscripciones.put(TiposEvento.LINEA_INICIADA, e -> {
            LineaDeOrdenId id = e.datosComo(LineaDeOrdenId.class);
            avanceCocina.lineaIniciada(id.ordenId(), id.lineaId());
        });
        suscripciones.put(TiposEvento.LINEA_LISTA, e -> {
            LineaDeOrdenId id = e.datosComo(LineaDeOrdenId.class);
            avanceCocina.lineaLista(id.ordenId(), id.lineaId());
        });
        suscripciones.put(TiposEvento.ORDEN_COBRADA,
                e -> registrarCobro.ordenCobrada(e.datosComo(OrdenPagada.class)));
        return suscripciones;
    }

    /** Dirección del servicio de Menú: --menu=URL, luego la variable MENU_URL y si no, la de por defecto. */
    public static String urlMenu(String[] args) {
        return new Opciones(args).texto("menu", "MENU_URL", URL_MENU_POR_DEFECTO);
    }
}
