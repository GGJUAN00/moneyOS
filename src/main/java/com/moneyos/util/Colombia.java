package com.moneyos.util;

import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Hora de Colombia y formato de pesos colombianos.
 * Toda la app usa la zona America/Bogota, aunque el servidor esté en otro país.
 */
public final class Colombia {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    public static final Locale ES_CO = Locale.forLanguageTag("es-CO");

    private Colombia() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static YearMonth mesActual() {
        return YearMonth.now(ZONA);
    }

    /** 104370000 → "$ 104.370.000" */
    public static String pesos(long valor) {
        NumberFormat f = NumberFormat.getIntegerInstance(ES_CO);
        return "$ " + f.format(Math.abs(valor));
    }

    /** Con signo: "+$ 300.000" o "-$ 25.000". */
    public static String pesosConSigno(long valor) {
        if (valor > 0) return "+" + pesos(valor);
        if (valor < 0) return "-" + pesos(valor);
        return pesos(0);
    }

    /** Versión corta: "$ 24,5M", "$ 376 mil". */
    public static String pesosCorto(long valor) {
        long abs = Math.abs(valor);
        NumberFormat un = NumberFormat.getNumberInstance(ES_CO);
        un.setMaximumFractionDigits(1);
        if (abs >= 1_000_000_000L) return "$ " + un.format(abs / 1_000_000_000.0) + " mil M";
        if (abs >= 1_000_000L) return "$ " + un.format(abs / 1_000_000.0) + "M";
        if (abs >= 1_000L) return "$ " + Math.round(abs / 1_000.0) + " mil";
        return "$ " + abs;
    }

    /** 3.6 → "3,6%" */
    public static String porcentaje(double valor) {
        NumberFormat f = NumberFormat.getNumberInstance(ES_CO);
        f.setMaximumFractionDigits(1);
        return f.format(valor) + "%";
    }

    /** "Hoy", "Ayer" o "mié, 23 de sept". */
    public static String diaRelativo(LocalDate fecha) {
        long dias = ChronoUnit.DAYS.between(fecha, hoy());
        if (dias == 0) return "Hoy";
        if (dias == 1) return "Ayer";
        String patron = fecha.getYear() == hoy().getYear() ? "EEE, d 'de' MMM" : "EEE, d 'de' MMM 'de' yyyy";
        return DateTimeFormatter.ofPattern(patron, ES_CO).format(fecha).replace(".", "");
    }

    /** "sábado, 21 de noviembre de 2026" */
    public static String fechaLarga(LocalDate fecha) {
        return DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES_CO).format(fecha);
    }

    /** "21/11/2026 3:42 p. m." */
    public static String fechaHora(LocalDateTime fechaHora) {
        if (fechaHora == null) return "—";
        return DateTimeFormatter.ofPattern("d/MM/yyyy h:mm a", ES_CO).format(fechaHora);
    }

    /** "Noviembre 2026" */
    public static String nombreMes(YearMonth mes) {
        return mayuscula(mes.getMonth().getDisplayName(TextStyle.FULL, ES_CO)) + " " + mes.getYear();
    }

    /** "Nov" */
    public static String mesCorto(YearMonth mes) {
        return mayuscula(mes.getMonth().getDisplayName(TextStyle.SHORT, ES_CO).replace(".", ""));
    }

    public static String saludo() {
        int h = ahora().getHour();
        if (h < 12) return "Buenos días";
        if (h < 19) return "Buenas tardes";
        return "Buenas noches";
    }

    public static String mayuscula(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(ES_CO) + s.substring(1);
    }

    /** Lee un monto escrito por la persona ("125.000", "$125000") → 125000. Devuelve -1 si no es válido. */
    public static long leerMonto(String texto) {
        if (texto == null) return -1;
        String digitos = texto.replaceAll("[^0-9]", "");
        if (digitos.isEmpty() || digitos.length() > 12) return -1;
        return Long.parseLong(digitos);
    }
}
