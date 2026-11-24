package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.util.List;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.DatosDemo;
import com.restaurante.infrastructure.config.Opciones;
import com.restaurante.infrastructure.eventos.ClienteBusEventos;
import com.restaurante.infrastructure.eventos.ReceptorEventosController;
import com.restaurante.infrastructure.in.web.http.ServidorApi;

/**
 * Arranca el servicio de Órdenes (puerto 8082): la API de órdenes y, hasta el Paso 7, la de caja.
 * La página web la sirve el gateway (8080). Opciones:
 * <ul>
 *   <li>--puerto=N  puerto HTTP (por defecto 8082)</li>
 *   <li>--demo      carga una orden de ejemplo (el Menú debe estar arrancado con --demo)</li>
 *   <li>--menu=URL  servicio de Menú (por defecto http://localhost:8081, o la variable MENU_URL)</li>
 *   <li>--bus=URL   bus de eventos (por defecto http://localhost:8090, o la variable BUS_URL)</li>
 * </ul>
 */
public class MainOrdenes {

    private static final int PUERTO_POR_DEFECTO = 8082;

    public static void main(String[] args) throws IOException {
        Opciones opciones = new Opciones(args);
        int puerto = opciones.puerto(PUERTO_POR_DEFECTO);
        String urlMenu = Aplicacion.urlMenu(args);
        ClienteBusEventos bus = new ClienteBusEventos(opciones.urlBus());

        Aplicacion app = Aplicacion.crear(urlMenu, opciones.urlBus());
        ServidorApi servidor = crearServidor(app, puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        bus.suscribirAlArrancar(List.copyOf(app.suscripciones().keySet()),
                opciones.urlPropia(servidor.getPuerto()) + ReceptorEventosController.RUTA);
        boolean demoCargado = opciones.tiene("demo") && DatosDemo.cargar(app);

        System.out.println("=== Servicio de Órdenes (con Caja) ===");
        System.out.println("API REST: http://localhost:" + servidor.getPuerto() + "/api/ordenes y /api/caja");
        System.out.println("Menú en:  " + urlMenu + "   Bus en: " + opciones.urlBus());
        if (demoCargado) {
            System.out.println("Orden de ejemplo cargada.");
        }
        System.out.println("Presiona Ctrl+C para detener el servicio.");
    }

    /** Los controladores de este proceso, incluido el que recibe eventos del bus. */
    public static ServidorApi crearServidor(Aplicacion app, int puerto) throws IOException {
        ReceptorEventosController receptor = new ReceptorEventosController();
        app.suscripciones().forEach(receptor::al);
        return new ServidorApi(List.of(
                new OrdenRestController(app.gestionarOrden()),
                new CajaRestController(app.cobrarOrden()),
                receptor), puerto);
    }
}
