package com.restaurante.infrastructure.in.web;

import com.restaurante.eventos.PublicadorEventosEnMemoria;
import com.restaurante.infrastructure.client.MenuRestClient;
import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.AplicacionCaja;
import com.restaurante.infrastructure.config.AplicacionCocina;
import com.restaurante.infrastructure.config.AplicacionMenu;
import com.restaurante.infrastructure.in.web.http.ServidorApi;
import java.io.IOException;
import java.util.List;

/**
 * Levanta, en puertos libres, los servicios reales de Menú, Cocina, Caja y Órdenes, y decide a
 * cuál va cada ruta (lo mismo que hace el gateway). Órdenes consulta al Menú por REST; Órdenes,
 * Cocina y Caja se avisan por un bus en memoria (el bus HTTP se prueba en su propio módulo).
 * El bus en memoria entrega al momento, así que aquí no hay que esperar a los eventos.
 */
final class ServiciosDePrueba {

    private final ServidorApi menu;
    private final ServidorApi cocina;
    private final ServidorApi caja;
    private final ServidorApi ordenes;
    private final String baseMenu;
    private final String baseCocina;
    private final String baseCaja;
    private final String baseOrdenes;

    ServiciosDePrueba() throws IOException {
        menu = new ServidorApi(List.of(new MenuRestController(AplicacionMenu.crear().administrarMenu())), 0);
        menu.iniciar();
        baseMenu = "http://localhost:" + menu.getPuerto();

        PublicadorEventosEnMemoria bus = new PublicadorEventosEnMemoria();
        AplicacionCocina appCocina = AplicacionCocina.crear(bus);
        AplicacionCaja appCaja = AplicacionCaja.crear(bus);
        Aplicacion appOrdenes = Aplicacion.crear(new MenuRestClient(baseMenu), bus);
        appCocina.suscripciones().forEach(bus::suscribir);
        appCaja.suscripciones().forEach(bus::suscribir);
        appOrdenes.suscripciones().forEach(bus::suscribir);

        cocina = new ServidorApi(List.of(new CocinaRestController(appCocina.gestionarCocina())), 0);
        cocina.iniciar();
        baseCocina = "http://localhost:" + cocina.getPuerto();

        caja = new ServidorApi(List.of(new CajaRestController(appCaja.cobrarOrden())), 0);
        caja.iniciar();
        baseCaja = "http://localhost:" + caja.getPuerto();

        ordenes = MainOrdenes.crearServidor(appOrdenes, 0);
        ordenes.iniciar();
        baseOrdenes = "http://localhost:" + ordenes.getPuerto();
    }

    /** URL completa: menú, cocina y caja van a su servicio; lo demás, a Órdenes (como el gateway). */
    String url(String ruta) {
        if (ruta.startsWith("/api/platos") || ruta.startsWith("/api/modificadores")) {
            return baseMenu + ruta;
        }
        if (ruta.startsWith("/api/cocina")) {
            return baseCocina + ruta;
        }
        if (ruta.startsWith("/api/caja")) {
            return baseCaja + ruta;
        }
        return baseOrdenes + ruta;
    }

    void detener() {
        ordenes.detener();
        caja.detener();
        cocina.detener();
        menu.detener();
    }
}
