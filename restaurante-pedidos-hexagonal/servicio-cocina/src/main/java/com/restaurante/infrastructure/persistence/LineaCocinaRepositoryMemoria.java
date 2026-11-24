package com.restaurante.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.restaurante.application.port.out.LineaCocinaRepository;
import com.restaurante.domain.LineaCocina;

/** Líneas de cocina en memoria, en el orden en que llegaron. */
public class LineaCocinaRepositoryMemoria implements LineaCocinaRepository {

    private final Map<String, LineaCocina> lineas = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(LineaCocina linea) {
        lineas.put(clave(linea.getOrdenId(), linea.getLineaId()), linea);
    }

    @Override
    public synchronized Optional<LineaCocina> buscar(String ordenId, String lineaId) {
        return Optional.ofNullable(lineas.get(clave(ordenId, lineaId)));
    }

    @Override
    public synchronized List<LineaCocina> listarDeOrden(String ordenId) {
        return lineas.values().stream().filter(l -> l.getOrdenId().equals(ordenId)).toList();
    }

    @Override
    public synchronized List<LineaCocina> listar() {
        return new ArrayList<>(lineas.values());
    }

    private static String clave(String ordenId, String lineaId) {
        return ordenId + "/" + lineaId;
    }
}
