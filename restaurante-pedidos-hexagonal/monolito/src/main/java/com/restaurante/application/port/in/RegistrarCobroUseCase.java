package com.restaurante.application.port.in;

import com.restaurante.eventos.DatosEventos.OrdenPagada;

/** Caja avisa por el bus (OrdenCobrada) que una orden se pagó; Órdenes la cierra en su lado. */
public interface RegistrarCobroUseCase {

    void ordenCobrada(OrdenPagada pago);
}
