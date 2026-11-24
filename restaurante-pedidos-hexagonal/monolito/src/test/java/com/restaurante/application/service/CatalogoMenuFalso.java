package com.restaurante.application.service;

import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** CatalogoMenu en memoria para probar Órdenes sin levantar el servicio de Menú. */
class CatalogoMenuFalso implements CatalogoMenu {

    private final Map<String, Plato> platos = new LinkedHashMap<>();
    private final Map<String, Modificador> modificadores = new LinkedHashMap<>();

    CatalogoMenuFalso con(Plato plato) {
        platos.put(plato.id(), plato);
        return this;
    }

    CatalogoMenuFalso con(Modificador modificador) {
        modificadores.put(modificador.id(), modificador);
        return this;
    }

    @Override
    public Optional<Plato> buscarPlato(String platoId) {
        return Optional.ofNullable(platos.get(platoId));
    }

    @Override
    public Optional<Modificador> buscarModificador(String modificadorId) {
        return Optional.ofNullable(modificadores.get(modificadorId));
    }

    @Override
    public List<Plato> listarPlatos() {
        return new ArrayList<>(platos.values());
    }

    @Override
    public List<Modificador> listarModificadores() {
        return new ArrayList<>(modificadores.values());
    }
}
