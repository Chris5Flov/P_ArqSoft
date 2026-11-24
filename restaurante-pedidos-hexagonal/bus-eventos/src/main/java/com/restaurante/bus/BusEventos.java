package com.restaurante.bus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.restaurante.eventos.Evento;
import com.restaurante.infrastructure.in.web.http.Json;

/**
 * El bus: guarda quién quiere cada tipo de evento y se lo reenvía por HTTP.
 *
 * Cada suscriptor (cada url) tiene su propia fila de entrega con un solo hilo, así los
 * eventos le llegan en el mismo orden en que se publicaron (por ejemplo, LineaCreada antes
 * que LineaCancelada) y un servicio lento no detiene a los demás.
 * Si un suscriptor no responde se reintenta 3 veces; después el evento se pierde para él
 * (se avisa en la consola). Para un proyecto escolar es suficiente.
 */
public class BusEventos {

    private static final int INTENTOS = 3;

    private final Map<String, Set<String>> urlsPorTipo = new LinkedHashMap<>();
    private final Map<String, ExecutorService> filaPorUrl = new ConcurrentHashMap<>();
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final long esperaEntreIntentosMs;

    public BusEventos() {
        this(500);
    }

    /** @param esperaEntreIntentosMs pausa antes de reintentar una entrega (las pruebas usan menos). */
    public BusEventos(long esperaEntreIntentosMs) {
        this.esperaEntreIntentosMs = esperaEntreIntentosMs;
    }

    /** Volver a suscribir la misma url al mismo tipo no la duplica. */
    public synchronized void suscribir(String tipo, String url) {
        urlsPorTipo.computeIfAbsent(tipo, t -> new LinkedHashSet<>()).add(url);
    }

    public synchronized void publicar(Evento evento) {
        String json = Json.escribir(evento);
        for (String url : urlsPorTipo.getOrDefault(evento.tipo(), Set.of())) {
            filaPorUrl.computeIfAbsent(url, u -> Executors.newSingleThreadExecutor())
                    .submit(() -> entregar(url, evento, json));
        }
    }

    public synchronized Map<String, List<String>> suscripciones() {
        Map<String, List<String>> copia = new LinkedHashMap<>();
        urlsPorTipo.forEach((tipo, urls) -> copia.put(tipo, List.copyOf(urls)));
        return copia;
    }

    public void detener() {
        filaPorUrl.values().forEach(ExecutorService::shutdown);
        for (ExecutorService fila : filaPorUrl.values()) {
            try {
                fila.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void entregar(String url, Evento evento, String json) {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
                if (respuesta.statusCode() < 500) {
                    if (respuesta.statusCode() >= 400) {
                        // El suscriptor recibió el evento pero lo rechazó: reintentar no cambiaría nada.
                        System.err.println("El suscriptor " + url + " rechazó " + evento.tipo() + " ("
                                + respuesta.statusCode() + "): " + respuesta.body());
                    }
                    return;
                }
            } catch (IOException e) {
                // no responde: se reintenta
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            dormir();
        }
        System.err.println("No se pudo entregar " + evento.tipo() + " (" + evento.id() + ") a " + url
                + " después de " + INTENTOS + " intentos");
    }

    private void dormir() {
        try {
            Thread.sleep(esperaEntreIntentosMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
