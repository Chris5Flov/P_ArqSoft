package com.restaurante.infrastructure.config;

import java.util.List;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.application.service.CobrarOrdenService;
import com.restaurante.application.service.GestionarCocinaService;
import com.restaurante.application.service.GestionarOrdenService;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoPasarelaSimuladoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;

/**
 * Raíz de composición: conecta los adaptadores de salida con los casos de uso.
 * La comparten todos los adaptadores de entrada (consola y web).
 *
 * El Menú ya se arma en su propio módulo (AplicacionMenu). Hasta el Paso 3, el monolito lo
 * incluye en el mismo proceso para que Órdenes siga buscando platos en sus repositorios.
 */
public record Aplicacion(AdministrarMenuUseCase administrarMenu,
                         GestionarOrdenUseCase gestionarOrden,
                         GestionarCocinaUseCase gestionarCocina,
                         CobrarOrdenUseCase cobrarOrden) {

    public static Aplicacion crear() {
        // 1. El Menú (armado por su módulo) y los adaptadores de salida de Órdenes, Cocina y Caja
        AplicacionMenu menu = AplicacionMenu.crear();
        OrdenRepository ordenRepository = new OrdenRepositoryMemoria();
        List<ProcesadorPago> procesadoresPago = List.of(
                new PagoEfectivoAdapter(),
                new PagoTarjetaSimuladoAdapter(),
                new PagoPasarelaSimuladoAdapter());

        // 2. Casos de uso (reciben los puertos de salida)
        return new Aplicacion(
                menu.administrarMenu(),
                new GestionarOrdenService(ordenRepository, menu.platoRepository(), menu.modificadorRepository()),
                new GestionarCocinaService(ordenRepository),
                new CobrarOrdenService(ordenRepository, procesadoresPago));
    }
}
