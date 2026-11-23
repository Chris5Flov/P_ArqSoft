package com.restaurante.infrastructure.in.web.dto;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.infrastructure.in.web.http.Formato;

/**
 * Cuerpos JSON de la API del servicio de Menú (ver docs/interfaz/contrato-api.md).
 * Los montos viajan como String con punto decimal y el tipo de modificador por su nombre.
 */
public final class MenuDto {

    private MenuDto() {
    }

    // ---------------------------------------------------------------- respuestas

    public record PlatoDto(String id, String nombre, String precioBase, String tasaImpuesto) { }

    public record ModificadorDto(String id, String nombre, String tipo, String costoExtra) { }

    // ---------------------------------------------------------------- solicitudes

    public record RegistrarPlatoRequest(String id, String nombre, String precioBase, String tasaImpuesto) { }

    public record ActualizarPlatoRequest(String nombre, String precioBase, String tasaImpuesto) { }

    public record RegistrarModificadorRequest(String id, String nombre, String tipo, String costoExtra) { }

    public record ActualizarModificadorRequest(String nombre, String tipo, String costoExtra) { }

    // ---------------------------------------------------------------- dominio -> DTO

    public static PlatoDto aDto(Plato plato) {
        return new PlatoDto(plato.id(), plato.nombre(),
                Formato.dinero(plato.precioBase()), Formato.tasa(plato.tasaImpuesto()));
    }

    public static ModificadorDto aDto(Modificador modificador) {
        return new ModificadorDto(modificador.id(), modificador.nombre(),
                Formato.nombre(modificador.tipo()), Formato.dinero(modificador.costoExtra()));
    }
}
