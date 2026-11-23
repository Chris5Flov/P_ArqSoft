package com.restaurante.infrastructure.in.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MenuDtoTest {

    @Test
    void mapeaUnPlato() {
        Plato tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
        assertEquals(new MenuDto.PlatoDto("P1", "Tacos", "85.00", "0.16"), MenuDto.aDto(tacos));
    }

    @Test
    void mapeaUnModificadorConSuTipoPorNombre() {
        Modificador sinCebolla = new Modificador("M3", "Sin cebolla", TipoModificador.EXCLUSION, BigDecimal.ZERO);
        assertEquals(new MenuDto.ModificadorDto("M3", "Sin cebolla", "EXCLUSION", "0.00"), MenuDto.aDto(sinCebolla));
    }
}
