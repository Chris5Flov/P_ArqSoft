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
    private boolean ordenCobrada; // la orden ya se pagó y se cerró en Caja

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
        exigirOrdenAbierta();
        if (estado != EstadoCocina.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "Solo se puede iniciar la cocción de una línea PENDIENTE (estado actual: " + estado + ")");
        }
        estado = EstadoCocina.EN_COCCION;
    }

    /** RF3: EN_COCCION -> LISTO. */
    public void marcarLista() {
        exigirOrdenAbierta();
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

    /** La orden se cobró (evento OrdenCobrada): la línea sale de la lista de la cocina. */
    public void cerrarPorCobro() {
        ordenCobrada = true;
    }

    /** PENDIENTE o EN_COCCION de una orden que sigue abierta: lo que la cocina todavía tiene que atender. */
    public boolean estaPorAtender() {
        return !ordenCobrada && (estado == EstadoCocina.PENDIENTE || estado == EstadoCocina.EN_COCCION);
    }

    private void exigirOrdenAbierta() {
        if (ordenCobrada) {
            throw new ReglaDeNegocioException("La orden " + ordenId + " ya se cobró y se cerró");
        }
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
