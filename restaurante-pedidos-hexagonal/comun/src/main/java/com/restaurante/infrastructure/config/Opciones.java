package com.restaurante.infrastructure.config;

/**
 * Lee las opciones de arranque de un servicio: primero el argumento (--bus=URL), luego la
 * variable de entorno (BUS_URL) y si no hay ninguno, el valor por defecto.
 */
public final class Opciones {

    public static final String BUS_POR_DEFECTO = "http://localhost:8090";

    private final String[] args;

    public Opciones(String[] args) {
        this.args = args == null ? new String[0] : args;
    }

    public String texto(String nombre, String variable, String porDefecto) {
        String valor = variable == null ? null : System.getenv(variable);
        String opcion = "--" + nombre + "=";
        for (String arg : args) {
            if (arg.startsWith(opcion)) {
                valor = arg.substring(opcion.length());
            }
        }
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    public int puerto(int porDefecto) {
        String valor = texto("puerto", null, String.valueOf(porDefecto));
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Puerto no válido: " + valor);
        }
    }

    public boolean tiene(String bandera) {
        for (String arg : args) {
            if (arg.equals("--" + bandera)) {
                return true;
            }
        }
        return false;
    }

    /** Dirección del bus de eventos: --bus=URL, BUS_URL o http://localhost:8090. */
    public String urlBus() {
        return texto("bus", "BUS_URL", BUS_POR_DEFECTO);
    }

    /** Dirección con la que los demás llegan a este servicio (para que el bus le avise). */
    public String urlPropia(int puerto) {
        return texto("url-propia", null, "http://localhost:" + puerto);
    }
}
