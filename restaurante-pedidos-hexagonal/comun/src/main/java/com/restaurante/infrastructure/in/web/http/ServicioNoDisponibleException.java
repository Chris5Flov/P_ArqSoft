package com.restaurante.infrastructure.in.web.http;

/**
 * Otro servicio del sistema no respondió (está apagado o falló).
 * La API la traduce a 503 para distinguirla de un error de la regla de negocio (400).
 */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
