package com.moneyos.modelo;

import jakarta.persistence.*;

/** Ajustes generales guardados como clave → valor (por ejemplo, el presupuesto mensual). */
@Entity
@Table(name = "ajustes")
public class Ajuste {

    @Id
    @Column(length = 40)
    private String clave;

    @Column(length = 200)
    private String valor;

    protected Ajuste() {
    }

    public Ajuste(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
