package com.restaurante.infrastructure.in.web.dto;

import static com.restaurante.infrastructure.in.web.http.Formato.dinero;
import static com.restaurante.infrastructure.in.web.http.Formato.fecha;
import static com.restaurante.infrastructure.in.web.http.Formato.nombre;

import com.restaurante.domain.Pago;
import com.restaurante.domain.Totales;

/** Lo que entra y sale por la API de Caja. Mismos campos que tenía cuando vivía en Órdenes. */
public final class CajaDto {

    private CajaDto() {
    }

    /** Cuerpo de POST /api/caja/ordenes/{id}/cobrar; descuento y propina son opcionales. */
    public record CobrarRequest(String metodo, String descuento, String propina) { }

    public record TotalesDto(String subtotal, String descuento, String impuestos, String propina, String total) { }

    public record PagoDto(String id, String ordenId, String fecha, String estado, String metodo,
                          String monto, String referencia) { }

    public static TotalesDto aDto(Totales t) {
        return new TotalesDto(dinero(t.subtotal()), dinero(t.descuento()), dinero(t.impuestos()),
                dinero(t.propina()), dinero(t.total()));
    }

    public static PagoDto aDto(Pago pago) {
        return new PagoDto(pago.id(), pago.ordenId(), fecha(pago.fecha()), nombre(pago.estado()),
                nombre(pago.metodo()), dinero(pago.monto()), pago.referencia());
    }
}
