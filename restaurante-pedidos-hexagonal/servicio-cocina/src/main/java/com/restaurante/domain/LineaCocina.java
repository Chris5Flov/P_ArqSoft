package com.restaurante.domain;

import java.util.List;

/**
 * Lo que Cocina sabe de una línea de pedido: qué preparar y en qué estado va.
 * No guarda precios: eso es de Órdenes y de Caja. La crea el evento LineaCreada.
 */
public class LineaCocina {

    private final String ordenId;
    private final String lineaId;
    private final String referenciaOrden;
    private final String plato;
    private int cantidad;
    private List<String> modificadores;
    private EstadoCocina estado = EstadoCocina.PENDIENTE;

    public LineaCocina(String ordenId, String lineaId, String referenciaOrden, String plato,
                       int cantidad, List<String> modificadores) {
        this.ordenId = ordenId;
        this.lineaId = lineaId;
        this.referenciaOrden = referenciaOrden;
        this.plato = plato;
        actualizar(cantidad, modificadores);
    }

    /** RF3: PENDIENTE -> EN_COCCION. Desde aquí el mesero ya no puede cambiarla. */
    public void iniciarCoccion() {
        if (estado != EstadoCocina.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "Solo se puede iniciar la cocción de una línea PENDIENTE (estado actual: " + estado + ")");
        }
        estado = EstadoCocina.EN_COCCION;
    }

    /** RF3: EN_COCCION -> LISTO. */
    public void marcarLista() {
        if (estado != EstadoCocina.EN_COCCION) {
            throw new ReglaDeNegocioException(
                    "Solo se puede marcar como lista una línea EN_COCCION (estado actual: " + estado + ")");
        }
        estado = EstadoCocina.LISTO;
    }

    /** El mesero cambió cantidad o modificadores (evento LineaModificada). */
    public void actualizar(int cantidad, List<String> modificadores) {
        exigirPendiente("modificar");
        this.cantidad = cantidad;
        this.modificadores = modificadores == null ? List.of() : List.copyOf(modificadores);
    }

    /** El mesero canceló la línea o la orden completa. */
    public void cancelar() {
        exigirPendiente("cancelar");
        estado = EstadoCocina.CANCELADA;
    }

    /** PENDIENTE o EN_COCCION: lo que la cocina todavía tiene que atender. */
    public boolean estaPorAtender() {
        return estado == EstadoCocina.PENDIENTE || estado == EstadoCocina.EN_COCCION;
    }

    private void exigirPendiente(String accion) {
        if (estado != EstadoCocina.PENDIENTE) {
            throw new ReglaDeNegocioException("No se puede " + accion
                    + " la línea: solo se permite mientras está PENDIENTE (estado actual: " + estado + ")");
        }
    }

    public String getOrdenId() { return ordenId; }
    public String getLineaId() { return lineaId; }
    public String getReferenciaOrden() { return referenciaOrden; }
    public String getPlato() { return plato; }
    public int getCantidad() { return cantidad; }
    public List<String> getModificadores() { return modificadores; }
    public EstadoCocina getEstado() { return estado; }
}
