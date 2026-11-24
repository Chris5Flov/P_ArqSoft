package com.restaurante.application.port.out;

import java.util.Optional;

import com.restaurante.domain.Orden;

/**
 * Las órdenes como las ve Caja: una copia de cada orden de Órdenes, armada con los eventos.
 * Lleva otro nombre que el OrdenRepository de Órdenes para que quede claro que no es el mismo.
 */
public interface OrdenCajaRepository {

    void guardar(Orden orden);

    Optional<Orden> buscarPorId(String id);
}
