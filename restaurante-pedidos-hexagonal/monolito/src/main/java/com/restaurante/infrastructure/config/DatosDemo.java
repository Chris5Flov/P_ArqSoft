package com.restaurante.infrastructure.config;

import java.util.List;

import com.restaurante.domain.Orden;
import com.restaurante.domain.TipoOrden;

/**
 * Carga un menú y una orden de ejemplo usando los casos de uso (para demostraciones y pruebas manuales).
 * Los platos y modificadores de ejemplo viven con el Menú (DatosDemoMenu).
 */
public final class DatosDemo {

    private DatosDemo() {
    }

    public static void cargar(Aplicacion app) {
        DatosDemoMenu.cargar(app.administrarMenu());

        Orden mesa = app.gestionarOrden().abrir(TipoOrden.SALA, "Mesa 5");
        app.gestionarOrden().agregarLinea(mesa.getId(), "P-001", 3, List.of("M-001", "M-003"));
        app.gestionarOrden().agregarLinea(mesa.getId(), "P-004", 2, List.of());
    }
}
