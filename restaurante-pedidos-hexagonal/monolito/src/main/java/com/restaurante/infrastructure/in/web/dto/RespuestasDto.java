package com.restaurante.infrastructure.in.web.dto;

import java.util.List;

/**
 * Cuerpos JSON que devuelve la API (ver docs/interfaz/contrato-api.md).
 * El dinero y las tasas viajan como String con punto decimal; los enums, por su nombre.
 */
public final class RespuestasDto {

    private RespuestasDto() {
    }

    /** Modificador dentro de una línea de pedido, con el costo extra con el que se aplicó. */
    public record ModificadorDto(String id, String nombre, String tipo, String costoExtra) { }

    public record TotalesDto(String subtotal, String descuento, String impuestos, String propina, String total) { }

    public record PagoDto(String id, String ordenId, String fecha, String estado, String metodo,
                          String monto, String referencia) { }

    public record LineaDto(String id, String platoId, String platoNombre, int cantidad,
                           String precioUnitario, String tasaImpuesto, List<ModificadorDto> modificadores,
                           String estadoCocina, boolean editable) { }

    public record OrdenDto(String id, String tipo, String referencia, String estado, String fechaCreacion,
                           List<LineaDto> lineas, TotalesDto totales, List<PagoDto> pagos) { }
}
