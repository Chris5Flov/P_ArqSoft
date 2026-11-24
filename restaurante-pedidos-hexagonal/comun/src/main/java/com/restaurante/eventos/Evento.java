package com.restaurante.eventos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.restaurante.infrastructure.in.web.http.Formato;
import com.restaurante.infrastructure.in.web.http.Json;

/**
 * Algo que ya pasó en un servicio y que a otros les puede interesar ("LineaCreada", "OrdenCobrada"...).
 * Viaja como JSON por el bus: {"id": "...", "tipo": "LineaCreada", "fecha": "...", "datos": {...}}.
 * Los datos son un record propio de cada tipo de evento.
 */
public record Evento(String id, String tipo, String fecha, JsonElement datos) {

    /** Crea un evento nuevo con id y fecha de ahora. */
    public static Evento nuevo(String tipo, Object datos) {
        return new Evento(UUID.randomUUID().toString(), tipo, Formato.fecha(LocalDateTime.now()),
                JsonParser.parseString(Json.escribir(datos)));
    }

    /** Lee los datos como el record que corresponde a este tipo de evento. */
    public <T> T datosComo(Class<T> tipoDatos) {
        return Json.leer(datos == null ? null : datos.toString(), tipoDatos);
    }
}
