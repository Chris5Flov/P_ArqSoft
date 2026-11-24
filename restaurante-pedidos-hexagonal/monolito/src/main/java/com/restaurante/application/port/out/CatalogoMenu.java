package com.restaurante.application.port.out;

import java.util.List;
import java.util.Optional;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;

/**
 * Puerto de salida de Órdenes para consultar el menú. Órdenes no sabe dónde vive el menú:
 * hoy el adaptador lo pide por REST al servicio de Menú (MenuRestClient).
 * Solo consulta; dar de alta o editar platos le toca al servicio de Menú.
 */
public interface CatalogoMenu {

    Optional<Plato> buscarPlato(String platoId);

    Optional<Modificador> buscarModificador(String modificadorId);

    List<Plato> listarPlatos();

    List<Modificador> listarModificadores();
}
