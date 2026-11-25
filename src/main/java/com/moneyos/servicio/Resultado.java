package com.moneyos.servicio;

/** Respuesta de una operación: si salió bien y el mensaje para mostrar (como un JOptionPane). */
public record Resultado(boolean exito, String mensaje) {

    public static Resultado bien(String mensaje) {
        return new Resultado(true, mensaje);
    }

    public static Resultado error(String mensaje) {
        return new Resultado(false, mensaje);
    }
}
