package com.restaurante.infrastructure.in.web;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.AplicacionMenu;
import com.restaurante.infrastructure.in.web.http.ServidorApi;
import java.io.IOException;
import java.util.List;

/**
 * Levanta, en puertos libres, el servicio de Menú real y el de Órdenes apuntando a él,
 * y decide a cuál va cada ruta (lo mismo que hará el gateway).
 */
final class ServiciosDePrueba {

    private final ServidorApi menu;
    private final ServidorWeb ordenes;
    private final String baseMenu;
    private final String baseOrdenes;

    ServiciosDePrueba() throws IOException {
        menu = new ServidorApi(List.of(new MenuRestController(AplicacionMenu.crear().administrarMenu())), 0);
        menu.iniciar();
        baseMenu = "http://localhost:" + menu.getPuerto();

        ordenes = new ServidorWeb(Aplicacion.crear(baseMenu), 0);
        ordenes.iniciar();
        baseOrdenes = "http://localhost:" + ordenes.getPuerto();
    }

    /** URL completa: /api/platos y /api/modificadores van al Menú; lo demás, a Órdenes. */
    String url(String ruta) {
        boolean esDelMenu = ruta.startsWith("/api/platos") || ruta.startsWith("/api/modificadores");
        return (esDelMenu ? baseMenu : baseOrdenes) + ruta;
    }

    void detener() {
        ordenes.detener();
        menu.detener();
    }
}
