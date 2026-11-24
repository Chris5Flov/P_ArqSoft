package com.restaurante.gateway;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tabla del gateway: qué servicio atiende cada prefijo de /api.
 * Ejemplo: "/api/platos" -> "http://localhost:8081".
 */
public final class RutasGateway {

    public static final String MENU_POR_DEFECTO = "http://localhost:8081";
    public static final String ORDENES_POR_DEFECTO = "http://localhost:8082";
    public static final String COCINA_POR_DEFECTO = "http://localhost:8083";
    public static final String CAJA_POR_DEFECTO = "http://localhost:8084";

    private final Map<String, String> servicioPorPrefijo = new LinkedHashMap<>();

    public RutasGateway agregar(String prefijo, String urlServicio) {
        String url = urlServicio.endsWith("/") ? urlServicio.substring(0, urlServicio.length() - 1) : urlServicio;
        servicioPorPrefijo.put(prefijo, url);
        return this;
    }

    /** Servicio que atiende la ruta: "/api/platos/P-001" coincide con el prefijo "/api/platos". */
    public Optional<String> servicioPara(String ruta) {
        return servicioPorPrefijo.entrySet().stream()
                .filter(e -> ruta.equals(e.getKey()) || ruta.startsWith(e.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Map<String, String> comoMapa() {
        return Map.copyOf(servicioPorPrefijo);
    }

    /**
     * Las rutas del sistema, una por servicio.
     * Cada dirección se toma del argumento (--menu=URL), luego de la variable (MENU_URL) y si no,
     * de la de por defecto.
     */
    public static RutasGateway delSistema(String[] args) {
        String menu = url(args, "menu", "MENU_URL", MENU_POR_DEFECTO);
        String ordenes = url(args, "ordenes", "ORDENES_URL", ORDENES_POR_DEFECTO);
        String cocina = url(args, "cocina", "COCINA_URL", COCINA_POR_DEFECTO);
        String caja = url(args, "caja", "CAJA_URL", CAJA_POR_DEFECTO);
        return new RutasGateway()
                .agregar("/api/platos", menu)
                .agregar("/api/modificadores", menu)
                .agregar("/api/ordenes", ordenes)
                .agregar("/api/cocina", cocina)
                .agregar("/api/caja", caja);
    }

    private static String url(String[] args, String nombre, String variable, String porDefecto) {
        String valor = System.getenv(variable);
        String opcion = "--" + nombre + "=";
        for (String arg : args) {
            if (arg.startsWith(opcion)) {
                valor = arg.substring(opcion.length());
            }
        }
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }
}
