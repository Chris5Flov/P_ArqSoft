package com.restaurante.infrastructure.eventos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.restaurante.eventos.Evento;
import com.restaurante.eventos.Suscriptor;
import com.restaurante.infrastructure.in.web.RestController;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;
import com.restaurante.infrastructure.in.web.http.SolicitudInvalidaException;

/**
 * Puerta de entrada de los eventos en un servicio: el bus hace POST /api/eventos/recibir
 * y este controlador se lo pasa a los suscriptores locales de ese tipo.
 * Es una ruta interna: el gateway no la expone.
 */
public class ReceptorEventosController implements RestController {

    public static final String RUTA = "/api/eventos/recibir";

    private final Map<String, List<Suscriptor>> suscriptores = new LinkedHashMap<>();

    public ReceptorEventosController al(String tipo, Suscriptor suscriptor) {
        suscriptores.computeIfAbsent(tipo, t -> new ArrayList<>()).add(suscriptor);
        return this;
    }

    /** Los tipos que escucha, para pedírselos al bus. */
    public List<String> tipos() {
        return List.copyOf(suscriptores.keySet());
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post(RUTA, this::recibir);
    }

    private Respuesta recibir(Peticion peticion) {
        Evento evento = peticion.cuerpoComo(Evento.class);
        if (evento.tipo() == null || evento.tipo().isBlank()) {
            throw new SolicitudInvalidaException("El evento necesita un tipo");
        }
        // Un tipo que no escucha se ignora: puede ser un evento nuevo que aún no le interesa.
        suscriptores.getOrDefault(evento.tipo(), List.of()).forEach(s -> s.recibir(evento));
        return Respuesta.sinContenido();
    }
}
