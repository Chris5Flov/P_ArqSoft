package com.restaurante.application.port.out;

import java.util.List;
import java.util.Optional;

import com.restaurante.domain.LineaCocina;

/** Las líneas que conoce Cocina. Es su propio almacenamiento: ya no lee el de Órdenes. */
public interface LineaCocinaRepository {

    void guardar(LineaCocina linea);

    Optional<LineaCocina> buscar(String ordenId, String lineaId);

    List<LineaCocina> listarDeOrden(String ordenId);

    /** En el orden en que llegaron. */
    List<LineaCocina> listar();
}
