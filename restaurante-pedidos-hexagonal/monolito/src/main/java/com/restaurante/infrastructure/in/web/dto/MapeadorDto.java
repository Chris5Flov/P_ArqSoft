package com.restaurante.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.ModificadorAplicado;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Totales;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.LineaCocinaDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.LineaDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.ModificadorDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.OrdenDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.PagoDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.TotalesDto;
import com.restaurante.infrastructure.in.web.http.Formato;

/**
 * Convierte objetos del dominio en los DTO del contrato de la API.
 * Los platos y modificadores del catálogo los mapea el servicio de Menú (MenuDto).
 */
public final class MapeadorDto {

    private MapeadorDto() {
    }

    /** Un modificador dentro de una línea lleva el costo extra con el que se aplicó, no el del catálogo. */
    public static ModificadorDto aDto(ModificadorAplicado aplicado) {
        Modificador m = aplicado.modificador();
        return new ModificadorDto(m.id(), m.nombre(), nombre(m.tipo()), dinero(aplicado.costoExtraAplicado()));
    }

    public static TotalesDto aDto(Totales t) {
        return new TotalesDto(dinero(t.subtotal()), dinero(t.descuento()), dinero(t.impuestos()),
                dinero(t.propina()), dinero(t.total()));
    }

    public static PagoDto aDto(Pago pago) {
        return new PagoDto(pago.id(), pago.ordenId(), fecha(pago.fecha()), nombre(pago.estado()),
                nombre(pago.metodo()), dinero(pago.monto()), pago.referencia());
    }

    public static LineaDto aDto(LineaPedido linea) {
        return new LineaDto(linea.getId(), linea.getPlato().id(), linea.getPlato().nombre(), linea.getCantidad(),
                dinero(linea.getPrecioUnitario()), tasa(linea.getTasaImpuestoAplicada()),
                linea.getModificadores().stream().map(MapeadorDto::aDto).toList(),
                nombre(linea.getEstadoCocina()), linea.esEditable());
    }

    /** Si la orden está cerrada lleva los totales cobrados; si no, los actuales sin descuento ni propina. */
    public static OrdenDto aDto(Orden orden) {
        Totales totales = orden.getTotales().orElseGet(orden::calcularTotales);
        return new OrdenDto(orden.getId(), nombre(orden.getTipo()), orden.getReferencia(), nombre(orden.getEstado()),
                fecha(orden.getFechaCreacion()),
                orden.getLineas().stream().map(MapeadorDto::aDto).toList(),
                aDto(totales),
                orden.getPagos().stream().map(MapeadorDto::aDto).toList());
    }

    public static LineaCocinaDto aDto(LineaEnCocina linea) {
        return new LineaCocinaDto(linea.ordenId(), linea.lineaId(), linea.referenciaOrden(), linea.plato(),
                linea.cantidad(), List.copyOf(linea.modificadores()), nombre(linea.estado()));
    }

    public static List<OrdenDto> ordenes(List<Orden> ordenes) {
        return ordenes.stream().map(MapeadorDto::aDto).toList();
    }

    // ---------------------------------------------------------------- formatos (ver Formato en comun)

    static String dinero(BigDecimal valor) {
        return Formato.dinero(valor);
    }

    static String tasa(BigDecimal valor) {
        return Formato.tasa(valor);
    }

    static String fecha(LocalDateTime valor) {
        return Formato.fecha(valor);
    }

    private static String nombre(Enum<?> valor) {
        return Formato.nombre(valor);
    }
}
