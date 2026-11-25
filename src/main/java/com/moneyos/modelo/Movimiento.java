package com.moneyos.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un ingreso o un gasto. Equivale a un "producto" del inventario de la guía. */
@Entity
@Table(name = "movimientos", indexes = @Index(name = "idx_movimientos_fecha", columnList = "fecha"))
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    /** Pesos colombianos, siempre positivo. */
    @Column(nullable = false)
    private long monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(length = 80)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Quién lo registró (queda vacío si ese usuario se elimina). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    protected Movimiento() {
    }

    public Movimiento(TipoMovimiento tipo, long monto, Categoria categoria, String descripcion, LocalDate fecha, Usuario registradoPor) {
        this.tipo = tipo;
        this.monto = monto;
        this.categoria = categoria;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.fecha = fecha;
        this.registradoPor = registradoPor;
        this.creadoEn = LocalDateTime.now();
    }

    /** Monto con signo: + para ingresos, − para gastos. */
    public long getMontoConSigno() {
        return tipo == TipoMovimiento.INGRESO ? monto : -monto;
    }

    public boolean esIngreso() {
        return tipo == TipoMovimiento.INGRESO;
    }

    /** Lo que se muestra como título: la descripción o, si no hay, la categoría. */
    public String getTitulo() {
        return descripcion == null || descripcion.isBlank() ? categoria.getNombre() : descripcion;
    }

    public Long getId() { return id; }
    public TipoMovimiento getTipo() { return tipo; }
    public long getMonto() { return monto; }
    public Categoria getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFecha() { return fecha; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
