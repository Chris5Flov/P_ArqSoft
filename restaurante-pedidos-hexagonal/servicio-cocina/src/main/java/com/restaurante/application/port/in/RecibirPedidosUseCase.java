package com.restaurante.application.port.in;

import com.restaurante.eventos.DatosEventos.LineaDeOrden;

/**
 * Lo que Órdenes le avisa a Cocina por el bus de eventos. Las entradas no vienen de una
 * persona sino de los eventos LineaCreada, LineaModificada, LineaCancelada y OrdenCancelada.
 */
public interface RecibirPedidosUseCase {

    void lineaCreada(LineaDeOrden linea);

    void lineaModificada(LineaDeOrden linea);

    void lineaCancelada(String ordenId, String lineaId);

    void ordenCancelada(String ordenId);

    /** La orden se pagó en Caja: sus líneas dejan de mostrarse en cocina. */
    void ordenCobrada(String ordenId);
}
