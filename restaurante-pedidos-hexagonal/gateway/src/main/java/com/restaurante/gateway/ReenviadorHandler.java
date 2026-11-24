package com.restaurante.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Atiende todo lo que llega a /api: mira el prefijo de la ruta, reenvía la petición tal cual
 * (método, cuerpo y query) al servicio que le toca y devuelve su respuesta sin cambiarla.
 * El gateway no sabe nada del negocio: solo enruta.
 */
public class ReenviadorHandler implements HttpHandler {

    private final RutasGateway rutas;
    private final HttpClient cliente;

    public ReenviadorHandler(RutasGateway rutas) {
        this.rutas = rutas;
        this.cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            String ruta = intercambio.getRequestURI().getRawPath();
            String metodo = intercambio.getRequestMethod();
            Optional<String> servicio = rutas.servicioPara(ruta);
            if (servicio.isEmpty()) {
                enviarError(intercambio, 404, "Ruta no encontrada: " + metodo + " " + ruta);
                return;
            }
            reenviar(intercambio, servicio.get());
        } finally {
            intercambio.close();
        }
    }

    private void reenviar(HttpExchange intercambio, String urlServicio) throws IOException {
        String query = intercambio.getRequestURI().getRawQuery();
        URI destino = URI.create(urlServicio + intercambio.getRequestURI().getRawPath()
                + (query == null ? "" : "?" + query));

        byte[] cuerpo;
        try (InputStream entrada = intercambio.getRequestBody()) {
            cuerpo = entrada.readAllBytes();
        }

        HttpRequest.Builder peticion = HttpRequest.newBuilder(destino)
                .timeout(Duration.ofSeconds(10))
                .method(intercambio.getRequestMethod(), cuerpo.length == 0
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(cuerpo));
        String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (tipo != null) {
            peticion.header("Content-Type", tipo);
        }

        HttpResponse<byte[]> respuesta;
        try {
            respuesta = cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            enviarError(intercambio, 503, "El servicio no responde: " + urlServicio);
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            enviarError(intercambio, 503, "Se interrumpió la petición a " + urlServicio);
            return;
        }

        respuesta.headers().firstValue("Content-Type")
                .ifPresent(valor -> intercambio.getResponseHeaders().set("Content-Type", valor));
        byte[] bytes = respuesta.body();
        if (bytes.length == 0) {
            intercambio.sendResponseHeaders(respuesta.statusCode(), -1);
            return;
        }
        intercambio.sendResponseHeaders(respuesta.statusCode(), bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    /** Mismo formato de error que los servicios: {"error": "..."}. */
    private static void enviarError(HttpExchange intercambio, int estado, String mensaje) throws IOException {
        String json = "{\"error\":\"" + mensaje.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
