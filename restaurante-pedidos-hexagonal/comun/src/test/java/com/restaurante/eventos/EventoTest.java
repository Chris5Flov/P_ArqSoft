package com.restaurante.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.restaurante.infrastructure.in.web.http.Json;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventoTest {

    private record LineaCreadaDePrueba(String ordenId, String lineaId, int cantidad, String precio, List<String> extras) { }

    @Test
    void unEventoNuevoTraeIdFechaYSusDatos() {
        Evento evento = Evento.nuevo(TiposEvento.LINEA_CREADA,
                new LineaCreadaDePrueba("O1", "L1", 3, "85.00", List.of("Queso")));

        assertNotNull(evento.id());
        assertNotNull(evento.fecha());
        assertEquals("LineaCreada", evento.tipo());
        assertEquals(new LineaCreadaDePrueba("O1", "L1", 3, "85.00", List.of("Queso")),
                evento.datosComo(LineaCreadaDePrueba.class));
    }

    @Test
    void viajaComoJsonConLosDatosAnidadosYVuelveIgual() {
        Evento evento = Evento.nuevo("LineaCreada", new LineaCreadaDePrueba("O1", "L1", 2, "10.50", List.of()));
        String json = Json.escribir(evento);

        JsonObject objeto = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("O1", objeto.getAsJsonObject("datos").get("ordenId").getAsString());
        assertEquals(2, objeto.getAsJsonObject("datos").get("cantidad").getAsInt());

        Evento deVuelta = Json.leer(json, Evento.class);
        assertEquals(evento.id(), deVuelta.id());
        assertEquals("10.50", deVuelta.datosComo(LineaCreadaDePrueba.class).precio());
    }

    @Test
    void elBusEnMemoriaEntregaSoloAQuienSeSuscribioYGuardaElOrden() {
        List<String> recibidos = new java.util.ArrayList<>();
        PublicadorEventosEnMemoria bus = new PublicadorEventosEnMemoria()
                .suscribir("LineaCreada", e -> recibidos.add("creada " + e.datosComo(LineaCreadaDePrueba.class).lineaId()))
                .suscribir("LineaCancelada", e -> recibidos.add("cancelada"));

        bus.publicar(Evento.nuevo("LineaCreada", new LineaCreadaDePrueba("O1", "L1", 1, "1", List.of())));
        bus.publicar(Evento.nuevo("OrdenCobrada", new LineaCreadaDePrueba("O1", "L1", 1, "1", List.of())));
        bus.publicar(Evento.nuevo("LineaCancelada", new LineaCreadaDePrueba("O1", "L1", 1, "1", List.of())));

        assertEquals(List.of("creada L1", "cancelada"), recibidos);
        assertEquals(3, bus.publicados().size());
        assertTrue(bus.publicados("OrdenCobrada").size() == 1);
    }
}
