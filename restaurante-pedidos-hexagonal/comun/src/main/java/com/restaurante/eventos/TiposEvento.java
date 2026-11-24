package com.restaurante.eventos;

/**
 * Nombres de los eventos del sistema, para no escribirlos a mano en cada servicio.
 *
 * <pre>
 * Evento          Lo publica   Le interesa a
 * OrdenCreada     Órdenes      Caja
 * LineaCreada     Órdenes      Cocina, Caja
 * LineaModificada Órdenes      Cocina, Caja
 * LineaCancelada  Órdenes      Cocina, Caja
 * OrdenCancelada  Órdenes      Cocina, Caja
 * LineaIniciada   Cocina       Órdenes
 * LineaLista      Cocina       Órdenes
 * OrdenCobrada    Caja         Órdenes, Cocina
 * </pre>
 */
public final class TiposEvento {

    public static final String ORDEN_CREADA = "OrdenCreada";
    public static final String LINEA_CREADA = "LineaCreada";
    public static final String LINEA_MODIFICADA = "LineaModificada";
    public static final String LINEA_CANCELADA = "LineaCancelada";
    public static final String ORDEN_CANCELADA = "OrdenCancelada";
    public static final String LINEA_INICIADA = "LineaIniciada";
    public static final String LINEA_LISTA = "LineaLista";
    public static final String ORDEN_COBRADA = "OrdenCobrada";

    private TiposEvento() {
    }
}
