package com.restaurante.infrastructure.config;

import java.math.BigDecimal;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;

/** Platos y modificadores de ejemplo que se cargan con --demo. */
public final class DatosDemoMenu {

    private DatosDemoMenu() {
    }

    public static void cargar(AdministrarMenuUseCase menu) {
        menu.registrarPlato(plato("P-001", "Tacos al pastor", "85.00", "0.16"));
        menu.registrarPlato(plato("P-002", "Pozole rojo", "120.00", "0.16"));
        menu.registrarPlato(plato("P-003", "Quesadilla", "60.00", "0.16"));
        menu.registrarPlato(plato("P-004", "Agua de horchata", "35.00", "0"));

        menu.registrarModificador(
                new Modificador("M-001", "Queso extra", TipoModificador.EXTRA, new BigDecimal("15.00")));
        menu.registrarModificador(
                new Modificador("M-002", "Aguacate", TipoModificador.EXTRA, new BigDecimal("20.00")));
        menu.registrarModificador(
                new Modificador("M-003", "Sin cebolla", TipoModificador.EXCLUSION, BigDecimal.ZERO));
    }

    private static Plato plato(String id, String nombre, String precio, String tasa) {
        return new Plato(id, nombre, new BigDecimal(precio), new BigDecimal(tasa));
    }
}
