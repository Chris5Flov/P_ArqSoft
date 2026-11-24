package com.restaurante.infrastructure.config;

import java.util.List;

import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.out.CatalogoMenu;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.application.service.CobrarOrdenService;
import com.restaurante.application.service.GestionarCocinaService;
import com.restaurante.application.service.GestionarOrdenService;
import com.restaurante.infrastructure.client.MenuRestClient;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoPasarelaSimuladoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;

/**
 * Raíz de composición de Órdenes: conecta los adaptadores de salida con los casos de uso.
 * La comparten la consola y la web.
 *
 * El menú ya no vive aquí: Órdenes lo consulta por REST al servicio de Menú (CatalogoMenu).
 * Cocina y Caja siguen en este mismo proceso y comparten el OrdenRepository a propósito,
 * hasta que se separen en sus propios servicios.
 */
public record Aplicacion(GestionarOrdenUseCase gestionarOrden,
                         GestionarCocinaUseCase gestionarCocina,
                         CobrarOrdenUseCase cobrarOrden) {

    public static final String URL_MENU_POR_DEFECTO = "http://localhost:8081";

    /** Usa la dirección del Menú de la variable MENU_URL, o http://localhost:8081. */
    public static Aplicacion crear() {
        return crear(urlMenu(new String[0]));
    }

    public static Aplicacion crear(String urlMenu) {
        return crear(new MenuRestClient(urlMenu));
    }

    public static Aplicacion crear(CatalogoMenu catalogoMenu) {
        // 1. Adaptadores de salida (menú por REST, persistencia en memoria y procesadores de pago)
        OrdenRepository ordenRepository = new OrdenRepositoryMemoria();
        List<ProcesadorPago> procesadoresPago = List.of(
                new PagoEfectivoAdapter(),
                new PagoTarjetaSimuladoAdapter(),
                new PagoPasarelaSimuladoAdapter());

        // 2. Casos de uso (reciben los puertos de salida)
        return new Aplicacion(
                new GestionarOrdenService(ordenRepository, catalogoMenu),
                new GestionarCocinaService(ordenRepository),
                new CobrarOrdenService(ordenRepository, procesadoresPago));
    }

    /** Dirección del servicio de Menú: --menu=URL, luego la variable MENU_URL y si no, la de por defecto. */
    public static String urlMenu(String[] args) {
        String valor = System.getenv("MENU_URL");
        for (String arg : args) {
            if (arg.startsWith("--menu=")) {
                valor = arg.substring("--menu=".length());
            }
        }
        return valor == null || valor.isBlank() ? URL_MENU_POR_DEFECTO : valor.trim();
    }
}
