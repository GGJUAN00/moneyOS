package com.moneyos.servicio;

import com.moneyos.modelo.EstadoUsuario;
import com.moneyos.modelo.Rol;
import com.moneyos.modelo.Usuario;
import com.moneyos.repositorio.MovimientoRepositorio;
import com.moneyos.repositorio.UsuarioRepositorio;
import com.moneyos.util.Colombia;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * La "libreta de usuarios" (DatosSistema en la guía): crear, buscar, eliminar y bloquear usuarios.
 */
@Service
public class UsuarioServicio {

    /** Minutos que dura el bloqueo de la cuenta después de 5 intentos fallidos seguidos. */
    public static final int MINUTOS_BLOQUEO = 15;
    public static final int CLAVE_MINIMA = 4;

    private static final Pattern USUARIO_VALIDO = Pattern.compile("^[a-zA-Z0-9._-]{3,40}$");
    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final UsuarioRepositorio usuarios;
    private final MovimientoRepositorio movimientos;
    private final PasswordEncoder cifrador;

    public UsuarioServicio(UsuarioRepositorio usuarios, MovimientoRepositorio movimientos, PasswordEncoder cifrador) {
        this.usuarios = usuarios;
        this.movimientos = movimientos;
        this.cifrador = cifrador;
    }

    public Optional<Usuario> buscar(String usuario) {
        if (usuario == null) return Optional.empty();
        return usuarios.findByUsuarioIgnoreCase(usuario.trim());
    }

    public Usuario obtener(String usuario) {
        return buscar(usuario).orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + usuario));
    }

    public List<Usuario> todos() {
        return usuarios.findAllByOrderByCreadoEnAsc();
    }

    /** Busca por usuario o nombre (Reto 2 de la guía, aplicado a usuarios). */
    public List<Usuario> buscarPorTexto(String texto) {
        List<Usuario> lista = todos();
        if (texto == null || texto.isBlank()) return lista;
        String buscado = texto.trim().toLowerCase();
        return lista.stream()
            .filter(u -> u.getUsuario().contains(buscado) || u.getNombreCompleto().toLowerCase().contains(buscado))
            .toList();
    }

    public long totalActivos() {
        return usuarios.countByEstado(EstadoUsuario.ACTIVO);
    }

    public long totalPendientes() {
        return usuarios.countByEstado(EstadoUsuario.PENDIENTE);
    }

    public boolean existeUsuario(String usuario) {
        return usuarios.existsByUsuarioIgnoreCase(usuario.trim());
    }

    // ---------- Crear ----------

    /** Crea un usuario activo (lo usa el ADMIN en "Gestión de Usuarios" y la carga inicial). */
    @Transactional
    public Resultado crearUsuario(String usuario, String clave, String nombreCompleto, Rol rol) {
        String u = usuario == null ? "" : usuario.trim();
        String c = clave == null ? "" : clave.trim();
        String n = nombreCompleto == null ? "" : nombreCompleto.trim();

        if (u.isEmpty() || c.isEmpty() || n.isEmpty() || rol == null) {
            return Resultado.error("Complete usuario, contraseña, nombre y rol.");
        }
        if (!USUARIO_VALIDO.matcher(u).matches()) {
            return Resultado.error("El usuario debe tener de 3 a 40 letras o números (sin espacios).");
        }
        if (c.length() < CLAVE_MINIMA) {
            return Resultado.error("La contraseña debe tener al menos " + CLAVE_MINIMA + " caracteres.");
        }
        if (existeUsuario(u)) {
            return Resultado.error("Ese nombre de usuario ya existe.");
        }
        usuarios.save(new Usuario(u, cifrador.encode(c), n, rol));
        return Resultado.bien("Usuario '" + u.toLowerCase() + "' creado con rol " + rol + ".");
    }

    /** Registro público ("Crear cuenta"): queda PENDIENTE hasta que el ADMIN lo apruebe. */
    @Transactional
    public Resultado registrar(String nombreCompleto, String usuario, String correo, String celular,
                               String clave, String confirmar, boolean aceptaDatos) {
        String n = nombreCompleto == null ? "" : nombreCompleto.trim();
        String u = usuario == null ? "" : usuario.trim();
        String e = correo == null ? "" : correo.trim().toLowerCase();
        String cel = celular == null ? "" : celular.replaceAll("\\D", "");
        String c = clave == null ? "" : clave;

        if (n.isEmpty() || u.isEmpty() || e.isEmpty() || cel.isEmpty() || c.isEmpty()) {
            return Resultado.error("Todos los campos son obligatorios.");
        }
        if (n.length() < 5 || !n.contains(" ")) {
            return Resultado.error("Escribe tu nombre completo (nombre y apellido).");
        }
        if (!USUARIO_VALIDO.matcher(u).matches()) {
            return Resultado.error("El usuario debe tener de 3 a 40 letras o números (sin espacios).");
        }
        if (!CORREO_VALIDO.matcher(e).matches()) {
            return Resultado.error("El correo no es válido.");
        }
        if (!cel.matches("^3\\d{9}$")) {
            return Resultado.error("El celular debe tener 10 dígitos y empezar por 3.");
        }
        if (c.length() < 6) {
            return Resultado.error("La contraseña debe tener al menos 6 caracteres.");
        }
        if (!c.equals(confirmar)) {
            return Resultado.error("Las contraseñas no coinciden.");
        }
        if (!aceptaDatos) {
            return Resultado.error("Debes autorizar el tratamiento de datos para continuar.");
        }
        if (existeUsuario(u)) {
            return Resultado.error("Ese nombre de usuario ya existe. Elige otro.");
        }

        Usuario nuevo = new Usuario(u, cifrador.encode(c), n, Rol.CONSULTA);
        nuevo.setCorreo(e);
        nuevo.setCelular(cel);
        nuevo.setEstado(EstadoUsuario.PENDIENTE);
        usuarios.save(nuevo);
        return Resultado.bien("Tu cuenta fue creada. Un administrador debe aprobarla antes de que puedas entrar.");
    }

    // ---------- Gestión (solo ADMIN) ----------

    /** Elimina un usuario. Como en la guía: no se puede eliminar la propia cuenta en sesión. */
    @Transactional
    public Resultado eliminar(Long id, String usuarioActual) {
        Optional<Usuario> elegido = usuarios.findById(id);
        if (elegido.isEmpty()) {
            return Resultado.error("Seleccione primero un usuario de la tabla.");
        }
        Usuario u = elegido.get();
        if (u.getUsuario().equalsIgnoreCase(usuarioActual)) {
            return Resultado.error("No puede eliminar su propia cuenta mientras está en sesión.");
        }
        if (u.getRol() == Rol.ADMIN && usuarios.countByRolAndEstado(Rol.ADMIN, EstadoUsuario.ACTIVO) <= 1) {
            return Resultado.error("No puede eliminar al único administrador del sistema.");
        }
        movimientos.quitarAutor(u);
        usuarios.delete(u);
        return Resultado.bien("Usuario '" + u.getUsuario() + "' eliminado.");
    }

    @Transactional
    public Resultado cambiarRol(Long id, Rol nuevoRol, String usuarioActual) {
        Optional<Usuario> elegido = usuarios.findById(id);
        if (elegido.isEmpty() || nuevoRol == null) {
            return Resultado.error("Seleccione un usuario y un rol.");
        }
        Usuario u = elegido.get();
        if (u.getUsuario().equalsIgnoreCase(usuarioActual) && nuevoRol != Rol.ADMIN) {
            return Resultado.error("No puede quitarse a sí mismo el rol de administrador.");
        }
        u.setRol(nuevoRol);
        return Resultado.bien("Ahora '" + u.getUsuario() + "' tiene el rol " + nuevoRol + ".");
    }

    @Transactional
    public Resultado aprobar(Long id, Rol rol) {
        Optional<Usuario> elegido = usuarios.findById(id);
        if (elegido.isEmpty()) {
            return Resultado.error("Usuario no encontrado.");
        }
        Usuario u = elegido.get();
        u.setEstado(EstadoUsuario.ACTIVO);
        u.setRol(rol == null ? Rol.CONSULTA : rol);
        return Resultado.bien("Cuenta de '" + u.getUsuario() + "' aprobada con rol " + u.getRol() + ".");
    }

    @Transactional
    public Resultado desbloquear(Long id) {
        Optional<Usuario> elegido = usuarios.findById(id);
        if (elegido.isEmpty()) {
            return Resultado.error("Usuario no encontrado.");
        }
        Usuario u = elegido.get();
        u.setBloqueadoHasta(null);
        u.setIntentosFallidos(0);
        return Resultado.bien("Usuario '" + u.getUsuario() + "' desbloqueado.");
    }

    // ---------- Inicio de sesión ----------

    /**
     * Suma un intento fallido a la cuenta. Al llegar a 5 seguidos la bloquea 15 minutos.
     * Devuelve cuántos intentos le quedan a esa cuenta (o -1 si el usuario no existe).
     */
    @Transactional
    public int registrarIntentoFallido(String usuario) {
        Optional<Usuario> encontrado = buscar(usuario);
        if (encontrado.isEmpty()) return -1;
        Usuario u = encontrado.get();
        int intentos = u.getIntentosFallidos() + 1;
        if (intentos >= Usuario.MAX_INTENTOS) {
            u.setBloqueadoHasta(Colombia.ahora().plusMinutes(MINUTOS_BLOQUEO));
            u.setIntentosFallidos(0);
            return 0;
        }
        u.setIntentosFallidos(intentos);
        return Usuario.MAX_INTENTOS - intentos;
    }

    @Transactional
    public void registrarIngreso(String usuario) {
        buscar(usuario).ifPresent(u -> {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
            u.setUltimoIngreso(Colombia.ahora());
        });
    }

    // ---------- Mi cuenta ----------

    @Transactional
    public Resultado cambiarClave(String usuario, String actual, String nueva, String confirmar) {
        Usuario u = obtener(usuario);
        if (actual == null || nueva == null || actual.isEmpty() || nueva.isEmpty()) {
            return Resultado.error("Escribe tu contraseña actual y la nueva.");
        }
        if (!cifrador.matches(actual, u.getClave())) {
            return Resultado.error("La contraseña actual no es correcta.");
        }
        if (nueva.length() < 6) {
            return Resultado.error("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (!nueva.equals(confirmar)) {
            return Resultado.error("Las contraseñas nuevas no coinciden.");
        }
        u.setClave(cifrador.encode(nueva));
        return Resultado.bien("Tu contraseña fue cambiada.");
    }

    @Transactional
    public void cambiarSonido(String usuario, boolean activo) {
        obtener(usuario).setSonido(activo);
    }

    @Transactional
    public void marcarResumenVisto(String usuario, String mes) {
        obtener(usuario).setResumenVisto(mes);
    }
}
