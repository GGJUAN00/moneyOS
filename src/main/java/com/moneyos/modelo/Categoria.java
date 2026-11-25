package com.moneyos.modelo;

import java.util.ArrayList;
import java.util.List;

/** Tipos de gasto y de ingreso, con su icono y su tono de gris para las gráficas. */
public enum Categoria {

    // ----- Gastos -----
    COMIDA("Comida", "🍜", "#F4F4F5", TipoMovimiento.GASTO, "Mercado, restaurantes, domicilios"),
    TRANSPORTE("Transporte", "🚇", "#CFCFD4", TipoMovimiento.GASTO, "TransMilenio, Uber, gasolina"),
    COMPRAS("Compras", "🛍️", "#ABABB1", TipoMovimiento.GASTO, "Ropa, tecnología, tiendas"),
    FACTURAS("Facturas", "⚡", "#8E8E95", TipoMovimiento.GASTO, "Servicios, arriendo, suscripciones"),
    ENTRETENIMIENTO("Entretenimiento", "🎬", "#C8C1B4", TipoMovimiento.GASTO, "Cine, salidas, juegos"),
    SALUD("Salud", "💊", "#9AA3AD", TipoMovimiento.GASTO, "Droguería, citas, EPS"),
    EDUCACION("Educación", "📚", "#727279", TipoMovimiento.GASTO, "Matrícula, cursos, libros"),
    HOGAR("Hogar", "🏠", "#B9B2A6", TipoMovimiento.GASTO, "Aseo, muebles, arreglos"),
    ENVIOS("Envíos", "💸", "#5F6670", TipoMovimiento.GASTO, "Plata que le mandas a alguien"),
    OTROS("Otros", "📦", "#4A4A50", TipoMovimiento.GASTO, ""),

    // ----- Ingresos -----
    SALDO_INICIAL("Saldo inicial", "🏦", "#F4F4F5", TipoMovimiento.INGRESO, "Lo que ya tienes en Nequi o el banco"),
    SALARIO("Salario", "💼", "#F4F4F5", TipoMovimiento.INGRESO, ""),
    ME_ENVIARON("Me enviaron", "📲", "#F4F4F5", TipoMovimiento.INGRESO, ""),
    OTRO_INGRESO("Otro ingreso", "✨", "#F4F4F5", TipoMovimiento.INGRESO, "");

    private final String nombre;
    private final String icono;
    private final String color;
    private final TipoMovimiento tipo;
    private final String pista;

    Categoria(String nombre, String icono, String color, TipoMovimiento tipo, String pista) {
        this.nombre = nombre;
        this.icono = icono;
        this.color = color;
        this.tipo = tipo;
        this.pista = pista;
    }

    public String getNombre() { return nombre; }
    public String getIcono() { return icono; }
    public String getColor() { return color; }
    public TipoMovimiento getTipo() { return tipo; }
    public String getPista() { return pista; }

    /** Las categorías de un tipo (para los selectores del formulario). */
    public static List<Categoria> delTipo(TipoMovimiento tipo) {
        List<Categoria> lista = new ArrayList<>();
        for (Categoria c : values()) {
            if (c.tipo == tipo) {
                lista.add(c);
            }
        }
        return lista;
    }
}
