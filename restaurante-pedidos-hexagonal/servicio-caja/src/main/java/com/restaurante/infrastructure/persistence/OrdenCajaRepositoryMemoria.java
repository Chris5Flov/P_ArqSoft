package com.restaurante.infrastructure.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.restaurante.application.port.out.OrdenCajaRepository;
import com.restaurante.domain.Orden;

/** Copias de las órdenes en memoria; se pierden al reiniciar Caja. */
public class OrdenCajaRepositoryMemoria implements OrdenCajaRepository {
    private final Map<String, Orden> datos = new ConcurrentHashMap<>();

    @Override
    public void guardar(Orden orden) {
        datos.put(orden.getId(), orden);
    }

    @Override
    public Optional<Orden> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }
}
