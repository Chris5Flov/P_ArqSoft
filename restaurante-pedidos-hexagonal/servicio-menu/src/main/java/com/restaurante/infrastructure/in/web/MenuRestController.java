package com.restaurante.infrastructure.in.web;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import com.restaurante.infrastructure.in.web.dto.MenuDto;
import com.restaurante.infrastructure.in.web.dto.MenuDto.ActualizarModificadorRequest;
import com.restaurante.infrastructure.in.web.dto.MenuDto.ActualizarPlatoRequest;
import com.restaurante.infrastructure.in.web.dto.MenuDto.RegistrarModificadorRequest;
import com.restaurante.infrastructure.in.web.dto.MenuDto.RegistrarPlatoRequest;
import com.restaurante.infrastructure.in.web.http.Conversion;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;

/** RF1 (Administrador): platos y modificadores del menú. */
public class MenuRestController implements RestController {

    private final AdministrarMenuUseCase administrarMenu;

    public MenuRestController(AdministrarMenuUseCase administrarMenu) {
        this.administrarMenu = administrarMenu;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador
                .get("/api/platos", this::listarPlatos)
                .post("/api/platos", this::registrarPlato)
                .put("/api/platos/{platoId}", this::actualizarPlato)
                .delete("/api/platos/{platoId}", this::eliminarPlato)
                .get("/api/modificadores", this::listarModificadores)
                .post("/api/modificadores", this::registrarModificador)
                .put("/api/modificadores/{modificadorId}", this::actualizarModificador)
                .delete("/api/modificadores/{modificadorId}", this::eliminarModificador);
    }

    private Respuesta listarPlatos(Peticion peticion) {
        return Respuesta.ok(administrarMenu.listarPlatos().stream().map(MenuDto::aDto).toList());
    }

    private Respuesta registrarPlato(Peticion peticion) {
        RegistrarPlatoRequest solicitud = peticion.cuerpoComo(RegistrarPlatoRequest.class);
        Plato plato = new Plato(solicitud.id(), solicitud.nombre(),
                Conversion.decimal(solicitud.precioBase(), "precioBase"),
                Conversion.decimal(solicitud.tasaImpuesto(), "tasaImpuesto"));
        return Respuesta.creado(MenuDto.aDto(administrarMenu.registrarPlato(plato)));
    }

    private Respuesta actualizarPlato(Peticion peticion) {
        ActualizarPlatoRequest solicitud = peticion.cuerpoComo(ActualizarPlatoRequest.class);
        Plato plato = new Plato(peticion.parametro("platoId"), solicitud.nombre(),
                Conversion.decimal(solicitud.precioBase(), "precioBase"),
                Conversion.decimal(solicitud.tasaImpuesto(), "tasaImpuesto"));
        return Respuesta.ok(MenuDto.aDto(administrarMenu.actualizarPlato(plato)));
    }

    private Respuesta eliminarPlato(Peticion peticion) {
        administrarMenu.eliminarPlato(peticion.parametro("platoId"));
        return Respuesta.sinContenido();
    }

    private Respuesta listarModificadores(Peticion peticion) {
        return Respuesta.ok(administrarMenu.listarModificadores().stream().map(MenuDto::aDto).toList());
    }

    private Respuesta registrarModificador(Peticion peticion) {
        RegistrarModificadorRequest solicitud = peticion.cuerpoComo(RegistrarModificadorRequest.class);
        Modificador modificador = new Modificador(solicitud.id(), solicitud.nombre(),
                Conversion.enumeracion(TipoModificador.class, solicitud.tipo(), "tipo"),
                Conversion.decimal(solicitud.costoExtra(), "costoExtra"));
        return Respuesta.creado(MenuDto.aDto(administrarMenu.registrarModificador(modificador)));
    }

    private Respuesta actualizarModificador(Peticion peticion) {
        ActualizarModificadorRequest solicitud = peticion.cuerpoComo(ActualizarModificadorRequest.class);
        Modificador modificador = new Modificador(peticion.parametro("modificadorId"), solicitud.nombre(),
                Conversion.enumeracion(TipoModificador.class, solicitud.tipo(), "tipo"),
                Conversion.decimal(solicitud.costoExtra(), "costoExtra"));
        return Respuesta.ok(MenuDto.aDto(administrarMenu.actualizarModificador(modificador)));
    }

    private Respuesta eliminarModificador(Peticion peticion) {
        administrarMenu.eliminarModificador(peticion.parametro("modificadorId"));
        return Respuesta.sinContenido();
    }
}
