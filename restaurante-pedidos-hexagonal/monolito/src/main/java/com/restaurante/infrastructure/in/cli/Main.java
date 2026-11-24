package com.restaurante.infrastructure.in.cli;

import java.util.Arrays;
import java.util.Scanner;

import com.restaurante.eventos.PublicadorEventosEnMemoria;
import com.restaurante.infrastructure.client.MenuRestClient;
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
        // La consola es una demostración independiente: guarda sus órdenes en su propia memoria y
        // no publica en el bus real (no se mezclan con las de la página web).
        Aplicacion app = Aplicacion.crear(new MenuRestClient(Aplicacion.urlMenu(args)), new PublicadorEventosEnMemoria());
        if (Arrays.asList(args).contains("--demo")) {
            DatosDemo.cargar(app);
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new MenuConsola(app.gestionarOrden(),
                    scanner, System.out).iniciar();
        }
    }
}
