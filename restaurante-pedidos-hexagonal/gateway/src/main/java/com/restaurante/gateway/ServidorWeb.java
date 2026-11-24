package com.restaurante.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

/**
 * Servidor del API Gateway: es la única entrada del sistema.
 * Sirve la interfaz web (src/main/resources/web/) en / y reenvía /api/... al servicio que
 * corresponda según {@link RutasGateway}. El frontend no cambia: sigue llamando a /api/...
 */
public class ServidorWeb {

    private final HttpServer servidor;
    private final ExecutorService hilos;

    /** @param puerto puerto TCP; 0 elige uno libre (útil en pruebas). */
    public ServidorWeb(RutasGateway rutas, int puerto) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/", new ReenviadorHandler(rutas));
        servidor.createContext("/", new ArchivosEstaticosHandler("web"));
        hilos = Executors.newFixedThreadPool(16);
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
