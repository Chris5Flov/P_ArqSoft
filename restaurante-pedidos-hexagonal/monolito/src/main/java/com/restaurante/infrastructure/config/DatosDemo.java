package com.restaurante.infrastructure.config;

import java.util.List;

import com.restaurante.domain.Orden;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.in.web.http.ServicioNoDisponibleException;

/**
 * Carga una orden de ejemplo usando los casos de uso (para demostraciones y pruebas manuales).
 * Sus platos los da el servicio de Menú, que debe estar arrancado con --demo.
 */
public final class DatosDemo {

    private DatosDemo() {
    }

    /** @return true si se cargó la orden; false si el servicio de Menú no respondió. */
    public static boolean cargar(Aplicacion app) {
        try {
            app.gestionarOrden().consultarPlatos(); // falla antes de abrir la orden si el Menú no responde
            Orden mesa = app.gestionarOrden().abrir(TipoOrden.SALA, "Mesa 5");
            app.gestionarOrden().agregarLinea(mesa.getId(), "P-001", 3, List.of("M-001", "M-003"));
            app.gestionarOrden().agregarLinea(mesa.getId(), "P-004", 2, List.of());
            return true;
        } catch (ServicioNoDisponibleException e) {
            System.err.println("No se cargó la orden de ejemplo: " + e.getMessage()
                    + ". Arranca primero el servicio de Menú con --demo.");
            return false;
        }
    }
}
