package com.moneyos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque de MoneyOS (igual que el método main de VentanaLogin en la guía).
 * Al ejecutarlo se enciende el servidor web y la app queda disponible en http://localhost:8080
 */
@SpringBootApplication
public class MoneyOsAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(MoneyOsAplicacion.class, args);
    }
}
