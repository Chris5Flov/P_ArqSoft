package com.restaurante.eventos;

import java.math.BigDecimal;
import java.util.List;

import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.ModificadorAplicado;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.infrastructure.in.web.http.Formato;

/**
 * Lo que lleva cada evento en "datos". Son el contrato entre servicios: quien publica llena
 * el record y quien escucha lo lee con evento.datosComo(...). Los montos viajan como texto.
 */
public final class DatosEventos {

    private DatosEventos() {
    }

    /** OrdenCreada: los datos de cabecera de la orden recién abierta. */
    public record OrdenAbierta(String ordenId, String tipo, String referencia, String fecha) { }

    /**
     * OrdenCobrada: el pago aprobado y con qué descuento y propina se cerró la orden.
     * Solo se publica cuando el pago se aprueba; los rechazados se quedan en Caja.
     */
    public record OrdenPagada(String ordenId,
                              String pagoId,
                              String fecha,
                              String metodo,
                              String monto,
                              String referenciaPago,
                              String descuento,
                              String propina) { }

    /** LineaCreada y LineaModificada: la línea completa, con el precio con el que se pidió. */
    public record LineaDeOrden(String ordenId,
                               String referenciaOrden,
                               String lineaId,
                               String platoId,
                               String platoNombre,
                               int cantidad,
                               String precioUnitario,
                               String tasaImpuesto,
                               List<ModificadorDeLinea> modificadores) { }

    public record ModificadorDeLinea(String id, String nombre, String tipo, String costoExtra) { }

    /** LineaCancelada, LineaIniciada y LineaLista: solo qué línea de qué orden. */
    public record LineaDeOrdenId(String ordenId, String lineaId) { }

    /** OrdenCancelada: solo qué orden. */
    public record OrdenId(String ordenId) { }

    public static OrdenAbierta ordenAbierta(Orden orden) {
        return new OrdenAbierta(orden.getId(), Formato.nombre(orden.getTipo()), orden.getReferencia(),
                Formato.fecha(orden.getFechaCreacion()));
    }

    public static OrdenPagada ordenPagada(Pago pago, BigDecimal descuento, BigDecimal propina) {
        return new OrdenPagada(pago.ordenId(), pago.id(), Formato.fecha(pago.fecha()), Formato.nombre(pago.metodo()),
                Formato.dinero(pago.monto()), pago.referencia(),
                Formato.dinero(descuento == null ? BigDecimal.ZERO : descuento),
                Formato.dinero(propina == null ? BigDecimal.ZERO : propina));
    }

    public static LineaDeOrden linea(Orden orden, LineaPedido linea) {
        List<ModificadorDeLinea> modificadores = linea.getModificadores().stream()
                .map(DatosEventos::modificador)
                .toList();
        return new LineaDeOrden(orden.getId(), orden.getReferencia(), linea.getId(),
                linea.getPlato().id(), linea.getPlato().nombre(), linea.getCantidad(),
                Formato.dinero(linea.getPrecioUnitario()), Formato.tasa(linea.getTasaImpuestoAplicada()),
                modificadores);
    }

    private static ModificadorDeLinea modificador(ModificadorAplicado m) {
        return new ModificadorDeLinea(m.modificador().id(), m.nombre(), Formato.nombre(m.modificador().tipo()),
                Formato.dinero(m.costoExtraAplicado()));
    }
}
