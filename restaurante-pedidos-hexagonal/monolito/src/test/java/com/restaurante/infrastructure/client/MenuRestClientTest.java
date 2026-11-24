package com.restaurante.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import com.restaurante.infrastructure.config.AplicacionMenu;
import com.restaurante.infrastructure.in.web.MenuRestController;
import com.restaurante.infrastructure.in.web.http.ServicioNoDisponibleException;
import com.restaurante.infrastructure.in.web.http.ServidorApi;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El adaptador contra el servicio de Menú real, en un puerto libre. */
class MenuRestClientTest {

    private ServidorApi servidorMenu;
    private MenuRestClient cliente;

    @BeforeEach
    void setUp() throws Exception {
        AplicacionMenu menu = AplicacionMenu.crear();
        menu.administrarMenu().registrarPlato(
                new Plato("P1", "Tacos", new BigDecimal("85.00"), new BigDecimal("0.16")));
        menu.administrarMenu().registrarModificador(
                new Modificador("M1", "Sin cebolla", TipoModificador.EXCLUSION, BigDecimal.ZERO));

        servidorMenu = new ServidorApi(List.of(new MenuRestController(menu.administrarMenu())), 0);
        servidorMenu.iniciar();
        cliente = new MenuRestClient("http://localhost:" + servidorMenu.getPuerto() + "/");
    }

    @AfterEach
    void tearDown() {
        servidorMenu.detener();
    }

    @Test
    void convierteElJsonDelMenuEnObjetosDelDominio() {
        Plato tacos = cliente.buscarPlato("P1").orElseThrow();
        assertEquals("Tacos", tacos.nombre());
        assertEquals(0, new BigDecimal("85").compareTo(tacos.precioBase()));
        assertEquals(0, new BigDecimal("0.16").compareTo(tacos.tasaImpuesto()));

        Modificador sinCebolla = cliente.buscarModificador("M1").orElseThrow();
        assertEquals(TipoModificador.EXCLUSION, sinCebolla.tipo());
        assertEquals(1, cliente.listarPlatos().size());
    }

    @Test
    void unIdQueNoExisteDaVacio() {
        assertFalse(cliente.buscarPlato("NO").isPresent());
        assertFalse(cliente.buscarModificador("NO").isPresent());
    }

    @Test
    void siElMenuNoRespondeLoDiceConClaridad() {
        servidorMenu.detener();

        ServicioNoDisponibleException e =
                assertThrows(ServicioNoDisponibleException.class, () -> cliente.listarPlatos());
        assertTrue(e.getMessage().contains("servicio de Menú"));
    }
}
