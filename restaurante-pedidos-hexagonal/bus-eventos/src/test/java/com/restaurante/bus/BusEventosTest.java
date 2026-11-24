package com.restaurante.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.eventos.Evento;
import com.restaurante.infrastructure.eventos.ClienteBusEventos;
import com.restaurante.infrastructure.eventos.ReceptorEventosController;
import com.restaurante.infrastructure.in.web.http.ServicioNoDisponibleException;
import com.restaurante.infrastructure.in.web.http.ServidorApi;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El bus real por HTTP, con dos "servicios" de prueba que solo anotan lo que les llega
 * (usan el mismo ReceptorEventosController que usarán Cocina, Caja y Órdenes).
 */
class BusEventosTest {

    private record Numero(int n) { }

    private BusEventos bus;
    private ServidorApi servidorBus;
    private ClienteBusEventos cliente;
    private final List<ServidorApi> servicios = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        bus = new BusEventos(50);
        servidorBus = new ServidorApi(List.of(new BusRestController(bus)), 0);
        servidorBus.iniciar();
        cliente = new ClienteBusEventos("http://localhost:" + servidorBus.getPuerto());
    }

    @AfterEach
    void tearDown() {
        servicios.forEach(ServidorApi::detener);
        servidorBus.detener();
        bus.detener();
    }

    /** Levanta un servicio de prueba, lo suscribe a esos tipos y devuelve dónde anota lo recibido. */
    private List<String> servicioSuscritoA(String... tipos) throws Exception {
        List<String> recibidos = Collections.synchronizedList(new ArrayList<>());
        ReceptorEventosController receptor = new ReceptorEventosController();
        for (String tipo : tipos) {
            receptor.al(tipo, e -> recibidos.add(e.tipo() + " " + e.datosComo(Numero.class).n()));
        }
        ServidorApi servicio = new ServidorApi(List.of(receptor), 0);
        servicio.iniciar();
        servicios.add(servicio);
        cliente.suscribir(receptor.tipos(),
                "http://localhost:" + servicio.getPuerto() + ReceptorEventosController.RUTA);
        return recibidos;
    }

    private static void esperarHasta(BooleanSupplier condicion) throws InterruptedException {
        for (int i = 0; i < 100 && !condicion.getAsBoolean(); i++) {
            Thread.sleep(30);
        }
    }

    @Test
    void entregaCadaEventoSoloAQuienSeSuscribioASuTipo() throws Exception {
        List<String> cocina = servicioSuscritoA("LineaCreada", "LineaCancelada");
        List<String> ordenes = servicioSuscritoA("LineaLista");

        cliente.publicar(Evento.nuevo("LineaCreada", new Numero(1)));
        cliente.publicar(Evento.nuevo("LineaLista", new Numero(2)));
        cliente.publicar(Evento.nuevo("OrdenCobrada", new Numero(3)));

        esperarHasta(() -> cocina.size() == 1 && ordenes.size() == 1);
        Thread.sleep(150); // por si llegara algo de más
        assertEquals(List.of("LineaCreada 1"), cocina);
        assertEquals(List.of("LineaLista 2"), ordenes);
    }

    @Test
    void losEventosLleganEnElOrdenEnQueSePublicaron() throws Exception {
        List<String> caja = servicioSuscritoA("LineaCreada", "LineaCancelada");
        List<String> esperado = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String tipo = i % 3 == 2 ? "LineaCancelada" : "LineaCreada";
            cliente.publicar(Evento.nuevo(tipo, new Numero(i)));
            esperado.add(tipo + " " + i);
        }

        esperarHasta(() -> caja.size() == 30);
        assertEquals(esperado, caja);
    }

    @Test
    void dosServiciosSuscritosAlMismoTipoRecibenElEvento() throws Exception {
        List<String> cocina = servicioSuscritoA("LineaCreada");
        List<String> caja = servicioSuscritoA("LineaCreada");

        cliente.publicar(Evento.nuevo("LineaCreada", new Numero(7)));

        esperarHasta(() -> cocina.size() == 1 && caja.size() == 1);
        assertEquals(List.of("LineaCreada 7"), cocina);
        assertEquals(List.of("LineaCreada 7"), caja);
    }

    @Test
    void unSuscriptorApagadoNoDetieneAlBusNiALosDemas() throws Exception {
        bus.suscribir("LineaCreada", "http://localhost:1/api/eventos/recibir"); // nadie escucha ahí
        List<String> caja = servicioSuscritoA("LineaCreada");

        cliente.publicar(Evento.nuevo("LineaCreada", new Numero(1)));

        esperarHasta(() -> caja.size() == 1);
        assertEquals(List.of("LineaCreada 1"), caja);
    }

    @Test
    void suscribirDosVecesNoDuplicaLaEntrega() throws Exception {
        List<String> caja = servicioSuscritoA("LineaCreada");
        String url = bus.suscripciones().get("LineaCreada").get(0);
        cliente.suscribir(List.of("LineaCreada"), url);

        cliente.publicar(Evento.nuevo("LineaCreada", new Numero(1)));

        esperarHasta(() -> caja.size() == 1);
        Thread.sleep(150);
        assertEquals(1, caja.size());
    }

    @Test
    void unEventoSinTipoSeRechazaCon400() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + servidorBus.getPuerto() + "/api/eventos"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"datos\":{}}")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("tipo"));
    }

    @Test
    void siElBusEstaApagadoPublicarLoAvisa() {
        servidorBus.detener();

        assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.publicar(Evento.nuevo("LineaCreada", new Numero(1))));
    }
}
