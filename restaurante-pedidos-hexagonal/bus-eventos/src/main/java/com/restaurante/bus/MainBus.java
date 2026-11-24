package com.restaurante.bus;

import java.io.IOException;
import java.util.List;

import com.restaurante.infrastructure.in.web.http.ServidorApi;

/**
 * Arranca el bus de eventos (puerto 8090). Conviene levantarlo antes que los servicios;
 * si no, ellos reintentan suscribirse hasta que esté listo.
 * Opción: --puerto=N.
 */
public class MainBus {

    private static final int PUERTO_POR_DEFECTO = 8090;

    public static void main(String[] args) throws IOException {
        int puerto = PUERTO_POR_DEFECTO;
        for (String arg : args) {
            if (arg.startsWith("--puerto=")) {
                puerto = Integer.parseInt(arg.substring("--puerto=".length()).trim());
            }
        }

        BusEventos bus = new BusEventos();
        ServidorApi servidor = new ServidorApi(List.of(new BusRestController(bus)), puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.detener();
            bus.detener();
        }));

        System.out.println("=== Bus de eventos ===");
        System.out.println("Publicar:     POST http://localhost:" + servidor.getPuerto() + "/api/eventos");
        System.out.println("Suscripciones: http://localhost:" + servidor.getPuerto() + "/api/suscripciones");
        System.out.println("Presiona Ctrl+C para detener el bus.");
    }
}
