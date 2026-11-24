package com.restaurante.eventos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bus dentro de un solo proceso: entrega cada evento en el momento a los suscriptores locales.
 * Sirve para las pruebas de un servicio sin levantar el bus de eventos real.
 */
public class PublicadorEventosEnMemoria implements PublicadorEventos {

    private final Map<String, List<Suscriptor>> suscriptores = new LinkedHashMap<>();
    private final List<Evento> publicados = new ArrayList<>();

    public PublicadorEventosEnMemoria suscribir(String tipo, Suscriptor suscriptor) {
        suscriptores.computeIfAbsent(tipo, t -> new ArrayList<>()).add(suscriptor);
        return this;
    }

    @Override
    public synchronized void publicar(Evento evento) {
        publicados.add(evento);
        suscriptores.getOrDefault(evento.tipo(), List.of()).forEach(s -> s.recibir(evento));
    }

    /** Todos los eventos publicados, en orden (para revisar en las pruebas). */
    public synchronized List<Evento> publicados() {
        return List.copyOf(publicados);
    }

    public synchronized List<Evento> publicados(String tipo) {
        return publicados.stream().filter(e -> e.tipo().equals(tipo)).toList();
    }
}
