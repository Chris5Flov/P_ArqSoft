package com.restaurante.infrastructure.eventos;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.restaurante.eventos.Evento;
import com.restaurante.eventos.PublicadorEventos;
import com.restaurante.infrastructure.in.web.http.Json;
import com.restaurante.infrastructure.in.web.http.ServicioNoDisponibleException;

/**
 * Habla con el bus de eventos (puerto 8090) por HTTP:
 * publicar es POST /api/eventos y suscribirse es POST /api/suscripciones.
 */
public class ClienteBusEventos implements PublicadorEventos {

    public static final String URL_POR_DEFECTO = "http://localhost:8090";

    /** Cuerpo de POST /api/suscripciones: "avísame a esta url cuando pase un evento de este tipo". */
    public record Suscripcion(String tipo, String url) { }

    private final String urlBus;
    private final HttpClient cliente;

    public ClienteBusEventos(String urlBus) {
        this.urlBus = urlBus.endsWith("/") ? urlBus.substring(0, urlBus.length() - 1) : urlBus;
        this.cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    /** @throws ServicioNoDisponibleException si el bus no responde. */
    @Override
    public void publicar(Evento evento) {
        enviar("/api/eventos", Json.escribir(evento));
    }

    /**
     * Pide al bus que mande los eventos de esos tipos a urlRetorno (el POST /api/eventos/recibir
     * de este servicio).
     * @throws ServicioNoDisponibleException si el bus no responde.
     */
    public void suscribir(List<String> tipos, String urlRetorno) {
        tipos.forEach(tipo -> enviar("/api/suscripciones", Json.escribir(new Suscripcion(tipo, urlRetorno))));
    }

    /**
     * Igual que suscribir, pero sin detener el arranque del servicio: si el bus todavía no está
     * levantado, lo reintenta en segundo plano cada 2 segundos hasta lograrlo.
     */
    public void suscribirAlArrancar(List<String> tipos, String urlRetorno) {
        Thread hilo = new Thread(() -> {
            boolean avisado = false;
            while (true) {
                try {
                    suscribir(tipos, urlRetorno);
                    if (avisado) {
                        System.out.println("Suscrito al bus de eventos en " + urlBus);
                    }
                    return;
                } catch (ServicioNoDisponibleException e) {
                    if (!avisado) {
                        System.out.println("El bus de eventos no responde en " + urlBus + "; reintentando...");
                        avisado = true;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interrumpido) {
                        return;
                    }
                }
            }
        }, "suscripcion-bus");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void enviar(String ruta, String json) {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBus + ruta))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 300) {
                throw new ServicioNoDisponibleException(
                        "El bus de eventos respondió " + respuesta.statusCode() + ": " + respuesta.body());
            }
        } catch (IOException e) {
            throw new ServicioNoDisponibleException("El bus de eventos no responde en " + urlBus, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("Se interrumpió el envío al bus de eventos", e);
        }
    }
}
