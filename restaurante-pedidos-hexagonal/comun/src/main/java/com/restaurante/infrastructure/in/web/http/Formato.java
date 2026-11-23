package com.restaurante.infrastructure.in.web.http;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cómo viajan los valores en el JSON de la API (ver docs/interfaz/contrato-api.md).
 * Lo comparten todos los servicios para que respondan con el mismo formato.
 */
public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Formato() {
    }

    /** Montos siempre con 2 decimales: "85.00". */
    public static String dinero(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Tasas como fracción sin ceros sobrantes: "0.16", "0". */
    public static String tasa(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return valor.signum() == 0 ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    /** ISO local sin zona y siempre con segundos: "2026-10-20T13:45:00". */
    public static String fecha(LocalDateTime valor) {
        return valor == null ? null : valor.format(FECHA);
    }

    /** Los enums viajan por su nombre: "EXTRA", "PENDIENTE". */
    public static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }
}
