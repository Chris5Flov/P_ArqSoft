package com.restaurante.application.port.in;

/**
 * Cocina avisa por el bus (LineaIniciada y LineaLista) y Órdenes actualiza su copia del estado
 * de la línea. Así Órdenes sigue aplicando RF3: una línea en cocción ya no se puede editar.
 */
public interface RegistrarAvanceCocinaUseCase {

    void lineaIniciada(String ordenId, String lineaId);

    void lineaLista(String ordenId, String lineaId);
}
