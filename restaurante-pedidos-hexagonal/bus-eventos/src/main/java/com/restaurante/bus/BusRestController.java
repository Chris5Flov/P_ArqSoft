package com.restaurante.bus;

import java.util.Map;

import com.restaurante.eventos.Evento;
import com.restaurante.infrastructure.eventos.ClienteBusEventos.Suscripcion;
import com.restaurante.infrastructure.in.web.RestController;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;
import com.restaurante.infrastructure.in.web.http.SolicitudInvalidaException;

/**
 * API del bus:
 * <ul>
 *   <li>POST /api/eventos        publica un evento (202: el bus lo entrega en segundo plano)</li>
 *   <li>POST /api/suscripciones  {"tipo": "LineaCreada", "url": "http://localhost:8083/api/eventos/recibir"}</li>
 *   <li>GET  /api/suscripciones  quién escucha qué (para revisar)</li>
 * </ul>
 */
public class BusRestController implements RestController {

    private final BusEventos bus;

    public BusRestController(BusEventos bus) {
        this.bus = bus;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador
                .post("/api/eventos", this::publicar)
                .post("/api/suscripciones", this::suscribir)
                .get("/api/suscripciones", p -> Respuesta.ok(bus.suscripciones()));
    }

    private Respuesta publicar(Peticion peticion) {
        Evento evento = peticion.cuerpoComo(Evento.class);
        exigir(evento.tipo(), "El evento necesita un tipo");
        bus.publicar(evento);
        return new Respuesta(202, Map.of("id", evento.id() == null ? "" : evento.id()));
    }

    private Respuesta suscribir(Peticion peticion) {
        Suscripcion suscripcion = peticion.cuerpoComo(Suscripcion.class);
        exigir(suscripcion.tipo(), "La suscripción necesita un tipo de evento");
        exigir(suscripcion.url(), "La suscripción necesita la url a la que se avisa");
        bus.suscribir(suscripcion.tipo(), suscripcion.url());
        return Respuesta.creado(suscripcion);
    }

    private static void exigir(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new SolicitudInvalidaException(mensaje);
        }
    }
}
