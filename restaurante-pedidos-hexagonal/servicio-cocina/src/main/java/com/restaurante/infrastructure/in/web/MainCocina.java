package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.util.List;

import com.restaurante.infrastructure.config.AplicacionCocina;
import com.restaurante.infrastructure.config.Opciones;
import com.restaurante.infrastructure.eventos.ClienteBusEventos;
import com.restaurante.infrastructure.eventos.ReceptorEventosController;
import com.restaurante.infrastructure.in.web.http.ServidorApi;

/**
 * Arranca el servicio de Cocina (puerto 8083). Recibe las líneas por el bus de eventos y
 * publica LineaIniciada y LineaLista. Opciones: --puerto=N y --bus=URL (por defecto
 * http://localhost:8090).
 */
public class MainCocina {

    private static final int PUERTO_POR_DEFECTO = 8083;

    public static void main(String[] args) throws IOException {
        Opciones opciones = new Opciones(args);
        int puerto = opciones.puerto(PUERTO_POR_DEFECTO);
        ClienteBusEventos bus = new ClienteBusEventos(opciones.urlBus());

        AplicacionCocina app = AplicacionCocina.crear(bus);
        ReceptorEventosController receptor = new ReceptorEventosController();
        app.suscripciones().forEach(receptor::al);

        ServidorApi servidor = new ServidorApi(List.of(new CocinaRestController(app.gestionarCocina()), receptor), puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        bus.suscribirAlArrancar(receptor.tipos(), opciones.urlPropia(servidor.getPuerto()) + ReceptorEventosController.RUTA);

        System.out.println("=== Servicio de Cocina ===");
        System.out.println("API REST: http://localhost:" + servidor.getPuerto() + "/api/cocina");
        System.out.println("Escucha:  " + String.join(", ", receptor.tipos()) + " (bus en " + opciones.urlBus() + ")");
        System.out.println("Presiona Ctrl+C para detener el servicio.");
    }
}
