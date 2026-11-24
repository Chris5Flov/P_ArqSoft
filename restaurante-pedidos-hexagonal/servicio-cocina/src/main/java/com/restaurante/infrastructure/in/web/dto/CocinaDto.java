package com.restaurante.infrastructure.in.web.dto;

import java.util.List;

import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.infrastructure.in.web.http.Formato;

/** Cuerpos JSON de la API del servicio de Cocina (ver docs/interfaz/contrato-api.md). */
public final class CocinaDto {

    private CocinaDto() {
    }

    public record LineaCocinaDto(String ordenId, String lineaId, String referenciaOrden, String plato,
                                 int cantidad, List<String> modificadores, String estado) { }

    public static LineaCocinaDto aDto(LineaEnCocina linea) {
        return new LineaCocinaDto(linea.ordenId(), linea.lineaId(), linea.referenciaOrden(), linea.plato(),
                linea.cantidad(), List.copyOf(linea.modificadores()), Formato.nombre(linea.estado()));
    }
}
