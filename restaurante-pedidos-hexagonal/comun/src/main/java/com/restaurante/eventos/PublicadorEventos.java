package com.restaurante.eventos;

/**
 * Puerto para avisar a los demás servicios que algo pasó. Quien publica no sabe quién escucha:
 * el bus se encarga de repartir el evento.
 */
public interface PublicadorEventos {

    void publicar(Evento evento);
}
