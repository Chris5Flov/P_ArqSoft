package com.restaurante.infrastructure.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.restaurante.infrastructure.config.Aplicacion;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Escenario que cruza Menú y Órdenes: una orden conserva sus líneas aunque después se
 * elimine el plato del menú. Vive en el monolito porque ahí siguen juntos los dos (Paso 2).
 */
class OrdenConMenuApiTest {

    private ServidorWeb servidor;
    private HttpClient cliente;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        servidor = new ServidorWeb(Aplicacion.crear(), 0);
        servidor.iniciar();
        cliente = HttpClient.newHttpClient();
        base = "http://localhost:" + servidor.getPuerto();

        assertEquals(201, pedir("POST", "/api/platos",
                "{\"id\":\"P1\",\"nombre\":\"Tacos\",\"precioBase\":\"85\",\"tasaImpuesto\":\"0.16\"}").statusCode());
        assertEquals(201, pedir("POST", "/api/modificadores",
                "{\"id\":\"M1\",\"nombre\":\"Queso\",\"tipo\":\"EXTRA\",\"costoExtra\":\"10\"}").statusCode());
    }

    @AfterEach
    void tearDown() {
        servidor.detener();
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String json) throws IOException, InterruptedException {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base + ruta));
        if (json == null) {
            peticion.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type", "application/json").method(metodo, HttpRequest.BodyPublishers.ofString(json));
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject objeto(HttpResponse<String> respuesta) {
        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    private JsonArray listar(String ruta) throws Exception {
        return JsonParser.parseString(pedir("GET", ruta, null).body()).getAsJsonArray();
    }

    @Test
    void unaOrdenConservaSusLineasAunqueSeElimineElPlato() throws Exception {
        String ordenId = objeto(pedir("POST", "/api/ordenes", "{\"tipo\":\"SALA\",\"referencia\":\"Mesa 1\"}"))
                .get("id").getAsString();
        assertEquals(200, pedir("POST", "/api/ordenes/" + ordenId + "/lineas",
                "{\"platoId\":\"P1\",\"cantidad\":1,\"modificadorIds\":[\"M1\"]}").statusCode());

        assertEquals(204, pedir("DELETE", "/api/platos/P1", null).statusCode());
        assertEquals(204, pedir("DELETE", "/api/modificadores/M1", null).statusCode());

        JsonObject orden = objeto(pedir("GET", "/api/ordenes/" + ordenId, null));
        JsonObject linea = orden.getAsJsonArray("lineas").get(0).getAsJsonObject();
        assertEquals("Tacos", linea.get("platoNombre").getAsString());
        assertEquals("110.20", orden.getAsJsonObject("totales").get("total").getAsString());

        HttpResponse<String> nuevaLinea = pedir("POST", "/api/ordenes/" + ordenId + "/lineas",
                "{\"platoId\":\"P1\",\"cantidad\":1,\"modificadorIds\":[]}");
        assertEquals(400, nuevaLinea.statusCode());
        assertEquals("No existe el plato P1", objeto(nuevaLinea).get("error").getAsString());
    }
}
