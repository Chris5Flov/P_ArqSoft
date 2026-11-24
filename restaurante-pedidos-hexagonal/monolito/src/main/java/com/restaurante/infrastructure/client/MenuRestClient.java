package com.restaurante.infrastructure.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import com.restaurante.infrastructure.in.web.http.Json;
import com.restaurante.infrastructure.in.web.http.ServicioNoDisponibleException;

/**
 * Adaptador de CatalogoMenu: consulta al servicio de Menú por su API REST
 * (GET /api/platos y GET /api/modificadores) con el HttpClient que trae Java 17.
 *
 * El menú es corto, así que para buscar por id pide la lista y filtra aquí.
 * Si el servicio no responde lanza ServicioNoDisponibleException (la API responde 503).
 */
public class MenuRestClient implements CatalogoMenu {

    /** Así viajan en el JSON del servicio de Menú (montos como texto, tipo por su nombre). */
    private record PlatoJson(String id, String nombre, String precioBase, String tasaImpuesto) { }

    private record ModificadorJson(String id, String nombre, String tipo, String costoExtra) { }

    private final String urlMenu;
    private final HttpClient cliente;

    /** @param urlMenu dirección base del servicio de Menú, por ejemplo http://localhost:8081 */
    public MenuRestClient(String urlMenu) {
        this.urlMenu = urlMenu.endsWith("/") ? urlMenu.substring(0, urlMenu.length() - 1) : urlMenu;
        this.cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public Optional<Plato> buscarPlato(String platoId) {
        return listarPlatos().stream().filter(p -> p.id().equals(platoId)).findFirst();
    }

    @Override
    public Optional<Modificador> buscarModificador(String modificadorId) {
        return listarModificadores().stream().filter(m -> m.id().equals(modificadorId)).findFirst();
    }

    @Override
    public List<Plato> listarPlatos() {
        PlatoJson[] platos = Json.leer(get("/api/platos"), PlatoJson[].class);
        return Arrays.stream(platos)
                .map(p -> new Plato(p.id(), p.nombre(), new BigDecimal(p.precioBase()), new BigDecimal(p.tasaImpuesto())))
                .toList();
    }

    @Override
    public List<Modificador> listarModificadores() {
        ModificadorJson[] modificadores = Json.leer(get("/api/modificadores"), ModificadorJson[].class);
        return Arrays.stream(modificadores)
                .map(m -> new Modificador(m.id(), m.nombre(), TipoModificador.valueOf(m.tipo()),
                        new BigDecimal(m.costoExtra())))
                .toList();
    }

    private String get(String ruta) {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlMenu + ruta))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new ServicioNoDisponibleException(
                        "El servicio de Menú respondió " + respuesta.statusCode() + " en " + ruta);
            }
            return respuesta.body();
        } catch (IOException e) {
            throw new ServicioNoDisponibleException("El servicio de Menú no responde en " + urlMenu, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("Se interrumpió la consulta al servicio de Menú", e);
        }
    }
}
