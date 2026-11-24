package com.restaurante.application.port.in;

import java.util.List;

import com.restaurante.domain.EstadoCocina;

/** RF3: la cocina consulta qué falta preparar y avanza el estado de cada línea. */
public interface GestionarCocinaUseCase {

    /** Líneas que la cocina aún tiene que atender (PENDIENTE o EN_COCCION), en el orden en que llegaron. */
    List<LineaEnCocina> listarPendientes();

    /** PENDIENTE -> EN_COCCION. */
    void iniciarCoccion(String ordenId, String lineaId);

    /** EN_COCCION -> LISTO. */
    void marcarLista(String ordenId, String lineaId);

    /** Vista de solo lectura de una línea, pensada para mostrarla en cocina. */
    record LineaEnCocina(String ordenId,
                         String lineaId,
                         String referenciaOrden,
                         String plato,
                         int cantidad,
                         List<String> modificadores,
                         EstadoCocina estado) { }
}
