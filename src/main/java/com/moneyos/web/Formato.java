package com.moneyos.web;

import com.moneyos.util.Colombia;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** Funciones de formato que usan las pantallas: ${@fmt.pesos(monto)}. */
@Component("fmt")
public class Formato {

    public String pesos(long v) { return Colombia.pesos(v); }

    public String conSigno(long v) { return Colombia.pesosConSigno(v); }

    public String corto(long v) { return (v < 0 ? "-" : "") + Colombia.pesosCorto(v); }

    public String pct(double v) { return Colombia.porcentaje(v); }

    public String dia(LocalDate f) { return Colombia.diaRelativo(f); }

    public String fechaHora(LocalDateTime f) { return Colombia.fechaHora(f); }

    public String mes(YearMonth m) { return Colombia.nombreMes(m); }

    public String hoyLargo() { return Colombia.mayuscula(Colombia.fechaLarga(Colombia.hoy())); }

    public String hoyIso() { return Colombia.hoy().toString(); }

    /** Milisegundos de diferencia entre el reloj de Colombia y UTC, para el reloj en vivo. */
    public long ahoraMs() { return System.currentTimeMillis(); }

    public String saludo() { return Colombia.saludo(); }
}
