package com.restaurante.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El gateway frente a dos servicios falsos que responden quién son y qué recibieron.
 * Así se prueba el reenvío sin depender de Menú ni de Órdenes.
 */
class GatewayTest {

    private HttpServer menuFalso;
    private HttpServer ordenesFalso;
    private ServidorWeb gateway;
    private HttpClient cliente;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        menuFalso = servicioQueResponde("menu");
        ordenesFalso = servicioQueResponde("ordenes");
        RutasGateway rutas = new RutasGateway()
                .agregar("/api/platos", "http://localhost:" + menuFalso.getAddress().getPort())
                .agregar("/api/ordenes", "http://localhost:" + ordenesFalso.getAddress().getPort() + "/")
                .agregar("/api/caja", "http://localhost:1"); // nadie escucha aquí

        gateway = new ServidorWeb(rutas, 0);
        gateway.iniciar();
        cliente = HttpClient.newHttpClient();
        base = "http://localhost:" + gateway.getPuerto();
    }

    @AfterEach
    void tearDown() {
        gateway.detener();
        menuFalso.stop(0);
        ordenesFalso.stop(0);
    }

    /** Responde 201 con "servicio METODO ruta?query cuerpo", salvo DELETE, que responde 204. */
    private static HttpServer servicioQueResponde(String nombre) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (intercambio.getRequestMethod().equals("DELETE")) {
                intercambio.sendResponseHeaders(204, -1);
                intercambio.close();
                return;
            }
            byte[] bytes = (nombre + " " + intercambio.getRequestMethod() + " " + intercambio.getRequestURI()
                    + " " + cuerpo).getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            intercambio.sendResponseHeaders(201, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
        return servidor;
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + ruta))
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo))
                .header("Content-Type", "application/json")
                .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void reenviaCadaPrefijoAlServicioQueLeToca() throws Exception {
        assertEquals("menu GET /api/platos ", pedir("GET", "/api/platos", null).body());
        assertEquals("ordenes GET /api/ordenes/abc/lineas ", pedir("GET", "/api/ordenes/abc/lineas", null).body());
    }

    @Test
    void conservaMetodoCuerpoQueryYCodigoDeRespuesta() throws Exception {
        HttpResponse<String> r = pedir("POST", "/api/ordenes?estado=ABIERTA", "{\"mesa\":5}");

        assertEquals(201, r.statusCode());
        assertEquals("ordenes POST /api/ordenes?estado=ABIERTA {\"mesa\":5}", r.body());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"));
        assertEquals(204, pedir("DELETE", "/api/platos/P-001", null).statusCode());
    }

    @Test
    void unPrefijoParecidoNoSeConfunde() throws Exception {
        assertEquals(404, pedir("GET", "/api/platosX", null).statusCode());
    }

    @Test
    void unaRutaSinServicioResponde404ConElFormatoDeLaApi() throws Exception {
        HttpResponse<String> r = pedir("GET", "/api/no-existe", null);

        assertEquals(404, r.statusCode());
        assertEquals("{\"error\":\"Ruta no encontrada: GET /api/no-existe\"}", r.body());
    }

    @Test
    void siElServicioEstaApagadoResponde503() throws Exception {
        HttpResponse<String> r = pedir("GET", "/api/caja/ordenes", null);

        assertEquals(503, r.statusCode());
        assertTrue(r.body().startsWith("{\"error\":\"El servicio no responde"));
    }

    @Test
    void laRaizEntregaLaPaginaHtml() throws Exception {
        HttpResponse<String> r = pedir("GET", "/", null);

        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertEquals(404, pedir("GET", "/no-existe.js", null).statusCode());
        assertEquals(404, pedir("GET", "/../pom.xml", null).statusCode());
    }

    @Test
    void porDefectoCadaPrefijoVaASuServicio() {
        RutasGateway rutas = RutasGateway.delSistema(new String[] {"--menu=http://m", "--ordenes=http://o"});

        assertEquals("http://m", rutas.servicioPara("/api/platos/P-001").orElseThrow());
        assertEquals("http://o", rutas.servicioPara("/api/ordenes").orElseThrow());
        assertEquals("http://localhost:8083", rutas.servicioPara("/api/cocina/pendientes").orElseThrow());
        assertEquals("http://localhost:8084", rutas.servicioPara("/api/caja/ordenes/1/totales").orElseThrow());
        assertTrue(rutas.servicioPara("/api/cajas").isEmpty());
    }
}
