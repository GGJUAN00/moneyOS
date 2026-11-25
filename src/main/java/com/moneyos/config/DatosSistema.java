package com.moneyos.config;

import com.moneyos.modelo.Rol;
import com.moneyos.repositorio.UsuarioRepositorio;
import com.moneyos.servicio.Resultado;
import com.moneyos.servicio.UsuarioServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Carga los usuarios iniciales la primera vez que arranca la app (cargarDatosIniciales de la guía).
 * Como ahora se guardan en base de datos, NO se pierden al cerrar el programa.
 */
@Component
public class DatosSistema implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosSistema.class);

    private final UsuarioRepositorio repositorio;
    private final UsuarioServicio usuarios;
    private final String adminUsuario;
    private final String adminClave;
    private final String adminNombre;
    private final boolean datosDemo;

    public DatosSistema(UsuarioRepositorio repositorio, UsuarioServicio usuarios,
                        @Value("${moneyos.admin.usuario}") String adminUsuario,
                        @Value("${moneyos.admin.clave}") String adminClave,
                        @Value("${moneyos.admin.nombre}") String adminNombre,
                        @Value("${moneyos.datos-demo}") boolean datosDemo) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.adminUsuario = adminUsuario;
        this.adminClave = adminClave;
        this.adminNombre = adminNombre;
        this.datosDemo = datosDemo;
    }

    @Override
    public void run(String... args) {
        cargarDatosIniciales();
    }

    public void cargarDatosIniciales() {
        // Bandera: si ya hay usuarios, no se vuelve a cargar nada
        if (repositorio.count() > 0) {
            return;
        }

        String clave = adminClave;
        if (clave == null || clave.isBlank()) {
            clave = claveAleatoria();
            log.warn("No se definió ADMIN_CLAVE. Clave temporal del administrador '{}': {}", adminUsuario, clave);
        }
        Resultado admin = usuarios.crearUsuario(adminUsuario, clave, adminNombre, Rol.ADMIN);
        log.info("Administrador inicial: {}", admin.mensaje());

        // Usuarios de prueba de la guía (solo en el computador, nunca en internet)
        if (datosDemo) {
            usuarios.crearUsuario("operador", "1234", "Laura Gómez", Rol.OPERADOR);
            usuarios.crearUsuario("consulta", "1234", "Andrés Pérez", Rol.CONSULTA);
            log.info("Usuarios de prueba creados: admin, operador y consulta (clave 1234)");
        }
    }

    private static String claveAleatoria() {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom azar = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(letras.charAt(azar.nextInt(letras.length())));
        return sb.toString();
    }
}
