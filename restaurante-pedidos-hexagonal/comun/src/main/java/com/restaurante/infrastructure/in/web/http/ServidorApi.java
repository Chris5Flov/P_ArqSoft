package com.restaurante.infrastructure.in.web.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.restaurante.infrastructure.in.web.RestController;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP del JDK que solo expone una API REST en /api, sin interfaz web.
 * Cada servicio lo arranca con sus propios controladores y en su propio puerto.
 */
public class ServidorApi {

    private final HttpServer servidor;
    private final ExecutorService hilos;

    /** Con puerto 0 el sistema elige uno libre (útil en las pruebas). */
    public ServidorApi(List<RestController> controladores, int puerto) throws IOException {
        Enrutador enrutador = new Enrutador();
        controladores.forEach(c -> c.registrarRutas(enrutador));

        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/", new ApiHandler(enrutador));
        hilos = Executors.newFixedThreadPool(8);
        servidor.setExecutor(hilos);
    }

    public void iniciar() {
        servidor.start();
    }

    public void detener() {
        servidor.stop(0);
        hilos.shutdownNow();
    }

    public int getPuerto() {
        return servidor.getAddress().getPort();
    }
}
