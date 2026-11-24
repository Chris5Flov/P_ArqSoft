package com.restaurante.eventos;

/** Lo que hace un servicio cuando le llega un evento al que se suscribió. */
@FunctionalInterface
public interface Suscriptor {

    void recibir(Evento evento);
}
