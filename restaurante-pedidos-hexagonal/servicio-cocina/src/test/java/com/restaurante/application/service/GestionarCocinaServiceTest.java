package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.domain.EstadoCocina;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.eventos.DatosEventos.LineaDeOrden;
import com.restaurante.eventos.DatosEventos.LineaDeOrdenId;
import com.restaurante.eventos.DatosEventos.ModificadorDeLinea;
import com.restaurante.eventos.PublicadorEventosEnMemoria;
import com.restaurante.eventos.TiposEvento;
import com.restaurante.infrastructure.persistence.LineaCocinaRepositoryMemoria;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cocina con su propio repositorio: las líneas le llegan como eventos de Órdenes. */
class GestionarCocinaServiceTest {

    private PublicadorEventosEnMemoria bus;
    private GestionarCocinaService servicio;

    @BeforeEach
    void setUp() {
        bus = new PublicadorEventosEnMemoria();
        servicio = new GestionarCocinaService(new LineaCocinaRepositoryMemoria(), bus);
        servicio.lineaCreada(tacos("O1", "L1", 2));
    }

    private static LineaDeOrden tacos(String ordenId, String lineaId, int cantidad) {
        return new LineaDeOrden(ordenId, "Mesa 5", lineaId, "P1", "Tacos", cantidad, "85.00", "0.16",
                List.of(new ModificadorDeLinea("M2", "Sin cebolla", "EXCLUSION", "0.00")));
    }

    @Test
    void listaLasLineasPendientesConSusDatos() {
        assertEquals(List.of(new LineaEnCocina("O1", "L1", "Mesa 5", "Tacos", 2,
                List.of("Sin cebolla"), EstadoCocina.PENDIENTE)), servicio.listarPendientes());
    }

    @Test
    void avanzaLaLineaHastaListoYDejaDeListarla() {
        servicio.iniciarCoccion("O1", "L1");
        assertEquals(EstadoCocina.EN_COCCION, servicio.listarPendientes().get(0).estado());

        servicio.marcarLista("O1", "L1");
        assertTrue(servicio.listarPendientes().isEmpty());
    }

    @Test
    void avisaPorElBusCadaAvanceParaQueOrdenesSeEntere() {
        servicio.iniciarCoccion("O1", "L1");
        servicio.marcarLista("O1", "L1");

        assertEquals(List.of(TiposEvento.LINEA_INICIADA, TiposEvento.LINEA_LISTA),
                bus.publicados().stream().map(e -> e.tipo()).toList());
        assertEquals(new LineaDeOrdenId("O1", "L1"), bus.publicados().get(1).datosComo(LineaDeOrdenId.class));
    }

    @Test
    void unaLineaModificadaSeActualizaMientrasSigaPendiente() {
        servicio.lineaModificada(new LineaDeOrden("O1", "Mesa 5", "L1", "P1", "Tacos", 4, "85.00", "0.16", List.of()));

        LineaEnCocina linea = servicio.listarPendientes().get(0);
        assertEquals(4, linea.cantidad());
        assertEquals(List.of(), linea.modificadores());
    }

    @Test
    void siElCambioLlegaCuandoYaSeEmpezoMandaCocina() {
        servicio.iniciarCoccion("O1", "L1");

        servicio.lineaModificada(tacos("O1", "L1", 9));
        servicio.lineaCancelada("O1", "L1");

        LineaEnCocina linea = servicio.listarPendientes().get(0);
        assertEquals(2, linea.cantidad());
        assertEquals(EstadoCocina.EN_COCCION, linea.estado());
    }

    @Test
    void noListaLineasCanceladas() {
        servicio.lineaCancelada("O1", "L1");
        assertTrue(servicio.listarPendientes().isEmpty());
    }

    @Test
    void cancelarLaOrdenQuitaTodasSusLineas() {
        servicio.lineaCreada(tacos("O1", "L2", 1));
        servicio.lineaCreada(tacos("O2", "L3", 1));

        servicio.ordenCancelada("O1");

        assertEquals(List.of("L3"), servicio.listarPendientes().stream().map(LineaEnCocina::lineaId).toList());
    }

    @Test
    void elMismoEventoDosVecesNoDuplicaLaLinea() {
        servicio.lineaCreada(tacos("O1", "L1", 2));
        assertEquals(1, servicio.listarPendientes().size());
    }

    @Test
    void rechazaIniciarLaCoccionDeUnaLineaCancelada() {
        servicio.lineaCancelada("O1", "L1");
        assertThrows(ReglaDeNegocioException.class, () -> servicio.iniciarCoccion("O1", "L1"));
        assertTrue(bus.publicados().isEmpty());
    }

    @Test
    void rechazaMarcarListaUnaLineaQueNoEstaEnCoccion() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.marcarLista("O1", "L1"));
        assertEquals(EstadoCocina.PENDIENTE, servicio.listarPendientes().get(0).estado());
    }

    @Test
    void rechazaUnaOrdenOUnaLineaQueNoExisten() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.iniciarCoccion("no-existe", "L1"));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.marcarLista("no-existe", "L1"));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.iniciarCoccion("O1", "no-existe"));
    }

    @Test
    void unaOrdenCobradaDejaDeVerseEnCocinaYYaNoSePuedeAvanzar() {
        servicio.lineaCreada(tacos("O1", "L2", 1));
        servicio.iniciarCoccion("O1", "L2");
        servicio.lineaCreada(tacos("O2", "L1", 1));

        servicio.ordenCobrada("O1");

        assertEquals(List.of("O2"), servicio.listarPendientes().stream().map(LineaEnCocina::ordenId).toList());
        assertThrows(ReglaDeNegocioException.class, () -> servicio.marcarLista("O1", "L2"));
    }
}
