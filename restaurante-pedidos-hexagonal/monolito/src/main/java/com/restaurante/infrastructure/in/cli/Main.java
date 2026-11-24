package com.restaurante.infrastructure.in.cli;

import java.util.Arrays;
import java.util.Scanner;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.DatosDemo;

/**
 * Punto de entrada de la consola. Los casos de uso se arman en {@link Aplicacion};
 * la interacción con el usuario vive en {@link MenuConsola}.
 * Opciones: --demo (carga una orden de ejemplo) y --menu=URL (servicio de Menú, por defecto
 * http://localhost:8081). El servicio de Menú debe estar arrancado.
 */
public class Main {
    public static void main(String[] args) {
        Aplicacion app = Aplicacion.crear(Aplicacion.urlMenu(args));
        if (Arrays.asList(args).contains("--demo")) {
            DatosDemo.cargar(app);
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new MenuConsola(app.gestionarOrden(), app.gestionarCocina(), app.cobrarOrden(),
                    scanner, System.out).iniciar();
        }
    }
}
