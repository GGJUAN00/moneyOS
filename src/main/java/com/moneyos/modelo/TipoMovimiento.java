package com.moneyos.modelo;

public enum TipoMovimiento {
    INGRESO("Ingreso"),
    GASTO("Gasto");

    private final String nombre;

    TipoMovimiento(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }
}
