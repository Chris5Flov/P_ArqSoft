package com.restaurante.gateway;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Arranca el API Gateway (puerto 8080): la página web y la única entrada a la API.
 * Opciones: --puerto=N y la dirección de cada servicio (--menu=URL, --ordenes=URL,
 * --cocina=URL, --caja=URL). Ver {@link RutasGateway}.
 */
public class MainGateway {

    private static final int PUERTO_POR_DEFECTO = 8080;

    public static void main(String[] args) throws IOException {
        int puerto = PUERTO_POR_DEFECTO;
        for (String arg : args) {
            if (arg.startsWith("--puerto=")) {
                puerto = Integer.parseInt(arg.substring("--puerto=".length()).trim());
            }
        }

        RutasGateway rutas = RutasGateway.delSistema(args);
        ServidorWeb servidor = new ServidorWeb(rutas, puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        System.out.println("=== Sistema de Pedidos de Restaurante: API Gateway ===");
        System.out.println("Interfaz web: http://localhost:" + servidor.getPuerto());
        Map<String, String> ordenadas = new TreeMap<>(rutas.comoMapa());
        ordenadas.forEach((prefijo, url) -> System.out.printf("  %-20s -> %s%n", prefijo, url));
        System.out.println("Presiona Ctrl+C para detener el gateway.");
    }
}
