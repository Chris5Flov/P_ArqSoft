package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.util.List;

import com.restaurante.infrastructure.config.AplicacionCaja;
import com.restaurante.infrastructure.config.Opciones;
import com.restaurante.infrastructure.eventos.ClienteBusEventos;
import com.restaurante.infrastructure.eventos.ReceptorEventosController;
import com.restaurante.infrastructure.in.web.http.ServidorApi;

/**
 * Arranca el servicio de Caja (puerto 8084). Arma su copia de cada orden con los eventos de
 * Órdenes y publica OrdenCobrada cuando un pago se aprueba. Opciones: --puerto=N y --bus=URL (por defecto
 * http://localhost:8090).
 */
public class MainCaja {

    private static final int PUERTO_POR_DEFECTO = 8084;

    public static void main(String[] args) throws IOException {
        Opciones opciones = new Opciones(args);
        int puerto = opciones.puerto(PUERTO_POR_DEFECTO);
        ClienteBusEventos bus = new ClienteBusEventos(opciones.urlBus());

        AplicacionCaja app = AplicacionCaja.crear(bus);
        ReceptorEventosController receptor = new ReceptorEventosController();
        app.suscripciones().forEach(receptor::al);

        ServidorApi servidor = new ServidorApi(List.of(new CajaRestController(app.cobrarOrden()), receptor), puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        bus.suscribirAlArrancar(receptor.tipos(), opciones.urlPropia(servidor.getPuerto()) + ReceptorEventosController.RUTA);

        System.out.println("=== Servicio de Caja ===");
        System.out.println("API REST: http://localhost:" + servidor.getPuerto() + "/api/caja");
        System.out.println("Escucha:  " + String.join(", ", receptor.tipos()) + " (bus en " + opciones.urlBus() + ")");
        System.out.println("Presiona Ctrl+C para detener el servicio.");
    }
}
