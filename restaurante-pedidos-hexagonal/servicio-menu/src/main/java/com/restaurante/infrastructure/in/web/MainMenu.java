package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.util.List;

import com.restaurante.infrastructure.config.AplicacionMenu;
import com.restaurante.infrastructure.config.DatosDemoMenu;
import com.restaurante.infrastructure.in.web.http.ServidorApi;

/**
 * Arranca el servicio de Menú como proceso propio: solo la API de platos y modificadores.
 * Opciones: --demo (carga el menú de ejemplo) y --puerto=N (por defecto 8081).
 */
public class MainMenu {

    private static final int PUERTO_POR_DEFECTO = 8081;

    public static void main(String[] args) throws IOException {
        int puerto = PUERTO_POR_DEFECTO;
        boolean demo = false;
        for (String arg : args) {
            if (arg.equals("--demo")) {
                demo = true;
            } else if (arg.startsWith("--puerto=")) {
                puerto = Integer.parseInt(arg.substring("--puerto=".length()).trim());
            }
        }

        AplicacionMenu app = AplicacionMenu.crear();
        if (demo) {
            DatosDemoMenu.cargar(app.administrarMenu());
        }

        ServidorApi servidor = new ServidorApi(List.of(new MenuRestController(app.administrarMenu())), puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        System.out.println("=== Servicio de Menú ===");
        System.out.println("API REST: http://localhost:" + servidor.getPuerto() + "/api/platos y /api/modificadores");
        if (demo) {
            System.out.println("Menú de ejemplo cargado.");
        }
        System.out.println("Presiona Ctrl+C para detener el servicio.");
    }
}
