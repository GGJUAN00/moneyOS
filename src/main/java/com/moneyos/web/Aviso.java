package com.moneyos.web;

import com.moneyos.servicio.Resultado;

/**
 * Mensaje emergente para la persona. Equivale al JOptionPane de la guía:
 * tiene título, mensaje y tipo (INFORMACION, ADVERTENCIA o ERROR).
 * "sonido" indica qué sonido suave reproducir (ingreso, gasto, borrar) o vacío.
 */
public record Aviso(String titulo, String mensaje, String tipo, String sonido) {

    public static Aviso informacion(String titulo, String mensaje) {
        return new Aviso(titulo, mensaje, "INFORMACION", "");
    }

    public static Aviso advertencia(String titulo, String mensaje) {
        return new Aviso(titulo, mensaje, "ADVERTENCIA", "");
    }

    public static Aviso error(String titulo, String mensaje) {
        return new Aviso(titulo, mensaje, "ERROR", "");
    }

    /** Convierte el resultado de una operación en aviso. */
    public static Aviso de(Resultado r, String tituloExito, String tituloError) {
        return r.exito() ? informacion(tituloExito, r.mensaje()) : error(tituloError, r.mensaje());
    }

    public Aviso conSonido(String sonido) {
        return new Aviso(titulo, mensaje, tipo, sonido);
    }
}
