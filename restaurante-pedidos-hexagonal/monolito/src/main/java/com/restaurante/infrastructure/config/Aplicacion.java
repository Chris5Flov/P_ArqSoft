package com.restaurante.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.in.RegistrarAvanceCocinaUseCase;
import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.application.service.CobrarOrdenService;
import com.restaurante.application.service.GestionarOrdenService;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.eventos.Suscriptor;
import com.restaurante.eventos.TiposEvento;
import com.restaurante.infrastructure.client.MenuRestClient;
import com.restaurante.infrastructure.eventos.ClienteBusEventos;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoPasarelaSimuladoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;

/**
 * Raíz de composición de Órdenes: conecta los adaptadores de salida con los casos de uso.
 * La comparten la consola y la web.
 *
 * El menú se consulta por REST al servicio de Menú (CatalogoMenu) y Cocina ya es otro servicio:
 * se entera de las líneas por el bus de eventos y avisa de vuelta cuando las avanza.
 * Caja sigue en este mismo proceso y comparte el OrdenRepository a propósito, hasta el Paso 7.
 */
public record Aplicacion(GestionarOrdenUseCase gestionarOrden,
                         RegistrarAvanceCocinaUseCase avanceCocina,
                         CobrarOrdenUseCase cobrarOrden) {

    public static final String URL_MENU_POR_DEFECTO = "http://localhost:8081";

    public static Aplicacion crear(String urlMenu, String urlBus) {
        return crear(new MenuRestClient(urlMenu), new ClienteBusEventos(urlBus));
    }

    public static Aplicacion crear(CatalogoMenu catalogoMenu, PublicadorEventos eventos) {
        // 1. Adaptadores de salida (menú por REST, persistencia en memoria y procesadores de pago)
        OrdenRepository ordenRepository = new OrdenRepositoryMemoria();
        List<ProcesadorPago> procesadoresPago = List.of(
                new PagoEfectivoAdapter(),
                new PagoTarjetaSimuladoAdapter(),
                new PagoPasarelaSimuladoAdapter());

        // 2. Casos de uso (reciben los puertos de salida)
        GestionarOrdenService ordenes = new GestionarOrdenService(ordenRepository, catalogoMenu, eventos);
        return new Aplicacion(
                ordenes,
                ordenes,
                new CobrarOrdenService(ordenRepository, procesadoresPago));
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
        return suscripciones;
    }

    /** Dirección del servicio de Menú: --menu=URL, luego la variable MENU_URL y si no, la de por defecto. */
    public static String urlMenu(String[] args) {
        return new Opciones(args).texto("menu", "MENU_URL", URL_MENU_POR_DEFECTO);
    }
}
