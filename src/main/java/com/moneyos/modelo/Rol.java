package com.moneyos.modelo;

/**
 * Los tres roles de la guía (el "gafete con llaves").
 * Cada rol decide qué pestañas del Panel Administrativo se muestran.
 *
 * | Rol       | Inicio | Movimientos | Registrar | Gestión de usuarios |
 * |-----------|--------|-------------|-----------|---------------------|
 * | ADMIN     |   Sí   |     Sí      |    Sí     |   Sí (exclusivo)    |
 * | OPERADOR  |   Sí   |     Sí      |    Sí     |         No          |
 * | CONSULTA  |   Sí   |     Sí      |    No     |         No          |
 */
public enum Rol {

    // Colores de la insignia, los mismos de la guía: rojo, verde azulado y azul
    ADMIN("Administrador", "#DC2626"),
    OPERADOR("Operador", "#0F766E"),
    CONSULTA("Consulta", "#2563EB");

    private final String nombre;
    private final String color;

    Rol(String nombre, String color) {
        this.nombre = nombre;
        this.color = color;
    }

    public String getNombre() { return nombre; }

    public String getColor() { return color; }

    /** Puede registrar y borrar movimientos (ADMIN y OPERADOR). */
    public boolean puedeRegistrar() {
        return this == ADMIN || this == OPERADOR;
    }

    /** Solo el ADMIN gestiona usuarios. */
    public boolean puedeGestionarUsuarios() {
        return this == ADMIN;
    }

    /** Texto de "Sus permisos activos según el rol" de la pestaña Inicio. */
    public String[] permisos() {
        switch (this) {
            case ADMIN:
                return new String[]{
                    "Consultar el saldo, los movimientos, las gráficas y los resúmenes.",
                    "Registrar ingresos y gastos.",
                    "Crear, aprobar y eliminar usuarios del sistema y asignar roles."
                };
            case OPERADOR:
                return new String[]{
                    "Consultar el saldo, los movimientos, las gráficas y los resúmenes.",
                    "Registrar ingresos y gastos.",
                    "(Restringido) No tiene acceso a la gestión de usuarios."
                };
            default:
                return new String[]{
                    "Consultar el saldo, los movimientos, las gráficas y los resúmenes.",
                    "(Restringido) No puede registrar movimientos ni gestionar usuarios."
                };
        }
    }
}
