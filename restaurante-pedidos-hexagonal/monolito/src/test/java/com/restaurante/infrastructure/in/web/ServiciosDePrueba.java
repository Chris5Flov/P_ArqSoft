package com.restaurante.infrastructure.in.web;

import com.restaurante.eventos.PublicadorEventosEnMemoria;
import com.restaurante.infrastructure.client.MenuRestClient;
import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.AplicacionCocina;
import com.restaurante.infrastructure.config.AplicacionMenu;
import com.restaurante.infrastructure.in.web.http.ServidorApi;
import java.io.IOException;
import java.util.List;

/**
 * Levanta, en puertos libres, los servicios reales de Menú, Cocina y Órdenes, y decide a cuál
 * va cada ruta (lo mismo que hace el gateway). Órdenes consulta al Menú por REST; Órdenes y
 * Cocina se avisan por un bus en memoria (el bus HTTP se prueba en su propio módulo).
 */
final class ServiciosDePrueba {

    private final ServidorApi menu;
    private final ServidorApi cocina;
    private final ServidorApi ordenes;
    private final String baseMenu;
    private final String baseCocina;
    private final String baseOrdenes;

    ServiciosDePrueba() throws IOException {
        menu = new ServidorApi(List.of(new MenuRestController(AplicacionMenu.crear().administrarMenu())), 0);
        menu.iniciar();
        baseMenu = "http://localhost:" + menu.getPuerto();

        PublicadorEventosEnMemoria bus = new PublicadorEventosEnMemoria();
        AplicacionCocina appCocina = AplicacionCocina.crear(bus);
        Aplicacion appOrdenes = Aplicacion.crear(new MenuRestClient(baseMenu), bus);
        appCocina.suscripciones().forEach(bus::suscribir);
        appOrdenes.suscripciones().forEach(bus::suscribir);

        cocina = new ServidorApi(List.of(new CocinaRestController(appCocina.gestionarCocina())), 0);
        cocina.iniciar();
        baseCocina = "http://localhost:" + cocina.getPuerto();

        ordenes = MainOrdenes.crearServidor(appOrdenes, 0);
        ordenes.iniciar();
        baseOrdenes = "http://localhost:" + ordenes.getPuerto();
    }

    /** URL completa: menú y cocina van a su servicio; lo demás, a Órdenes. */
    String url(String ruta) {
        if (ruta.startsWith("/api/platos") || ruta.startsWith("/api/modificadores")) {
            return baseMenu + ruta;
        }
        if (ruta.startsWith("/api/cocina")) {
            return baseCocina + ruta;
        }
        return baseOrdenes + ruta;
    }

    void detener() {
        ordenes.detener();
        cocina.detener();
        menu.detener();
    }
}
