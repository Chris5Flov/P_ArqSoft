package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.util.List;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.DatosDemo;
import com.restaurante.infrastructure.in.web.http.ServidorApi;

/**
 * Arranca el servicio de Órdenes (puerto 8082): solo la API de órdenes, cocina y caja.
 * La página web la sirve el gateway (8080). Opciones:
 * <ul>
 *   <li>--puerto=N  puerto HTTP (por defecto 8082)</li>
 *   <li>--demo      carga una orden de ejemplo (el Menú debe estar arrancado con --demo)</li>
 *   <li>--menu=URL  servicio de Menú (por defecto http://localhost:8081, o la variable MENU_URL)</li>
 * </ul>
 */
public class MainOrdenes {

    private static final int PUERTO_POR_DEFECTO = 8082;

    public static void main(String[] args) throws IOException {
        int puerto = PUERTO_POR_DEFECTO;
        boolean demo = false;
        for (String arg : args) {
            if (arg.equals("--demo")) {
                demo = true;
            } else if (arg.startsWith("--puerto=")) {
                puerto = Integer.parseInt(arg.substring("--puerto=".length()).trim());
            }
        }

        String urlMenu = Aplicacion.urlMenu(args);
        Aplicacion app = Aplicacion.crear(urlMenu);
        boolean demoCargado = demo && DatosDemo.cargar(app);

        ServidorApi servidor = crearServidor(app, puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        System.out.println("=== Servicio de Órdenes (con Cocina y Caja) ===");
        System.out.println("API REST: http://localhost:" + servidor.getPuerto() + "/api/ordenes, /api/cocina y /api/caja");
        System.out.println("Menú en:  " + urlMenu);
        if (demoCargado) {
            System.out.println("Orden de ejemplo cargada.");
        }
        System.out.println("Presiona Ctrl+C para detener el servicio.");
    }

    /** Los controladores de este proceso. Cocina y Caja siguen aquí hasta separarse. */
    public static ServidorApi crearServidor(Aplicacion app, int puerto) throws IOException {
        return new ServidorApi(List.of(
                new OrdenRestController(app.gestionarOrden()),
                new CocinaRestController(app.gestionarCocina()),
                new CajaRestController(app.cobrarOrden())), puerto);
    }
}
