package com.moneyos.seguridad;

import jakarta.servlet.http.HttpSession;

import java.time.Instant;

/**
 * Contador de intentos fallidos de esta "ventana" de login (la sesión del navegador).
 * En la guía era el arreglo intentosFallidos = {0}; al llegar a 3 se bloquea el formulario.
 */
public final class IntentosLogin {

    public static final int MAXIMO = 3;
    public static final int SEGUNDOS_BLOQUEO = 5 * 60;

    private static final String INTENTOS = "moneyos.intentos";
    private static final String BLOQUEO = "moneyos.bloqueoHasta";

    private IntentosLogin() {
    }

    public static int fallidos(HttpSession s) {
        Object v = s.getAttribute(INTENTOS);
        return v instanceof Integer i ? i : 0;
    }

    public static int disponibles(HttpSession s) {
        return MAXIMO - fallidos(s);
    }

    /** Suma un fallo y devuelve cuántos intentos quedan. Al llegar a 0 bloquea la ventana. */
    public static int sumarFallo(HttpSession s) {
        int fallos = fallidos(s) + 1;
        if (fallos >= MAXIMO) {
            s.setAttribute(BLOQUEO, Instant.now().plusSeconds(SEGUNDOS_BLOQUEO).toEpochMilli());
            s.setAttribute(INTENTOS, MAXIMO);
            return 0;
        }
        s.setAttribute(INTENTOS, fallos);
        return MAXIMO - fallos;
    }

    /** Milisegundos que faltan para desbloquear (0 si no está bloqueada). Si ya pasó el tiempo, reinicia. */
    public static long bloqueoRestante(HttpSession s) {
        Object v = s.getAttribute(BLOQUEO);
        if (!(v instanceof Long hasta)) return 0;
        long falta = hasta - System.currentTimeMillis();
        if (falta <= 0) {
            reiniciar(s);
            return 0;
        }
        return falta;
    }

    public static void reiniciar(HttpSession s) {
        s.removeAttribute(INTENTOS);
        s.removeAttribute(BLOQUEO);
    }
}
