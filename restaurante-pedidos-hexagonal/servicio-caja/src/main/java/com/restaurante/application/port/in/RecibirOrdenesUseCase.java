package com.restaurante.application.port.in;

import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.OrdenAbierta;

/** Lo que Caja hace con los eventos de Órdenes para tener al día su copia de cada orden. */
public interface RecibirOrdenesUseCase {

    void ordenCreada(OrdenAbierta orden);

    void lineaCreada(LineaDeOrden linea);

    void lineaModificada(LineaDeOrden linea);

    void lineaCancelada(String ordenId, String lineaId);

    void ordenCancelada(String ordenId);
}
