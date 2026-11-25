package com.moneyos.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Un usuario del sistema. En la guía cada usuario ocupaba la misma posición en los arreglos
 * paralelos (usuarios[i], claves[i], nombres[i], roles[i]); aquí es una fila de la tabla "usuarios".
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    /**
     * Intentos fallidos seguidos antes de bloquear LA CUENTA (protege contra quien intente adivinar
     * contraseñas desde muchos navegadores). La ventana de login se bloquea antes, a los 3 (como en la guía).
     */
    public static final int MAX_INTENTOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario para entrar. Se guarda en minúsculas: "ADMIN" y "admin" son el mismo. */
    @Column(nullable = false, unique = true, length = 40)
    private String usuario;

    /** Contraseña cifrada con BCrypt (nunca se guarda ni se muestra tal cual). */
    @Column(nullable = false)
    private String clave;

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @Column(length = 120)
    private String correo;

    @Column(length = 20)
    private String celular;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    private int intentosFallidos;

    /** Si tiene fecha y aún no ha pasado, la cuenta está bloqueada. */
    private LocalDateTime bloqueadoHasta;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    private LocalDateTime ultimoIngreso;

    /** Último resumen mensual que vio (formato 2026-10). */
    @Column(length = 7)
    private String resumenVisto;

    private boolean sonido = true;

    protected Usuario() {
    }

    public Usuario(String usuario, String claveCifrada, String nombreCompleto, Rol rol) {
        this.usuario = usuario.trim().toLowerCase();
        this.clave = claveCifrada;
        this.nombreCompleto = nombreCompleto.trim();
        this.rol = rol;
        this.creadoEn = LocalDateTime.now();
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    /** "Carlos Mendoza" → "CM" (para el círculo del perfil). */
    public String getIniciales() {
        String[] partes = nombreCompleto.trim().split("\\s+");
        String a = partes[0].substring(0, 1);
        String b = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }

    /** Primer nombre, para saludar. */
    public String getPrimerNombre() {
        return nombreCompleto.trim().split("\\s+")[0];
    }

    // ----- getters y setters -----

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getUltimoIngreso() { return ultimoIngreso; }
    public void setUltimoIngreso(LocalDateTime ultimoIngreso) { this.ultimoIngreso = ultimoIngreso; }
    public String getResumenVisto() { return resumenVisto; }
    public void setResumenVisto(String resumenVisto) { this.resumenVisto = resumenVisto; }
    public boolean isSonido() { return sonido; }
    public void setSonido(boolean sonido) { this.sonido = sonido; }
}
