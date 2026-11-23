package com.restaurante.infrastructure.config;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.out.ModificadorRepository;
import com.restaurante.application.port.out.PlatoRepository;
import com.restaurante.application.service.AdministrarMenuService;
import com.restaurante.infrastructure.persistence.ModificadorRepositoryMemoria;
import com.restaurante.infrastructure.persistence.PlatoRepositoryMemoria;

/**
 * Arma el servicio de Menú: crea sus repositorios y se los pasa al caso de uso.
 *
 * Los repositorios se exponen solo porque, hasta el Paso 3, el monolito los sigue usando
 * para que Órdenes busque platos en el mismo proceso. Cuando Órdenes consulte a Menú por
 * REST (puerto CatalogoMenu) bastará con el caso de uso.
 */
public record AplicacionMenu(AdministrarMenuUseCase administrarMenu,
                             PlatoRepository platoRepository,
                             ModificadorRepository modificadorRepository) {

    public static AplicacionMenu crear() {
        PlatoRepository platoRepository = new PlatoRepositoryMemoria();
        ModificadorRepository modificadorRepository = new ModificadorRepositoryMemoria();
        return new AplicacionMenu(
                new AdministrarMenuService(platoRepository, modificadorRepository),
                platoRepository,
                modificadorRepository);
    }
}
