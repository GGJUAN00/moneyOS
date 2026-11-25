package com.moneyos.web;

import com.moneyos.modelo.Rol;
import com.moneyos.modelo.Usuario;
import com.moneyos.servicio.FinanzasServicio;
import com.moneyos.servicio.Resultado;
import com.moneyos.servicio.UsuarioServicio;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Los botones del panel: guardar movimiento, eliminar, crear usuario, cambiar rol, etc.
 * Cada acción vuelve al panel con un aviso (como el JOptionPane de la guía).
 */
@Controller
public class AccionesControlador {

    private final UsuarioServicio usuarios;
    private final FinanzasServicio finanzas;

    public AccionesControlador(UsuarioServicio usuarios, FinanzasServicio finanzas) {
        this.usuarios = usuarios;
        this.finanzas = finanzas;
    }

    // ===================== Movimientos (ADMIN y OPERADOR) =====================

    @PostMapping("/panel/movimientos")
    public String registrarMovimiento(@RequestParam(defaultValue = "") String tipo,
                                      @RequestParam(defaultValue = "") String monto,
                                      @RequestParam(defaultValue = "") String categoria,
                                      @RequestParam(defaultValue = "") String descripcion,
                                      @RequestParam(defaultValue = "") String fecha,
                                      Principal principal, RedirectAttributes avisos) {
        Usuario actual = usuarios.obtener(principal.getName());
        Resultado r = finanzas.registrar(actual, tipo, monto, categoria, descripcion, fecha);
        Aviso aviso = r.exito()
            ? Aviso.informacion("Registro exitoso", r.mensaje()).conSonido("INGRESO".equals(tipo) ? "ingreso" : "gasto")
            : Aviso.advertencia("Validación", r.mensaje());
        avisos.addFlashAttribute("aviso", aviso);
        return r.exito() ? "redirect:/panel?pestana=inicio" : "redirect:/panel?pestana=registrar&tipo=" + tipo;
    }

    @PostMapping("/panel/movimientos/{id}/eliminar")
    public String eliminarMovimiento(@PathVariable Long id, Principal principal, RedirectAttributes avisos) {
        Resultado r = finanzas.eliminar(id, usuarios.obtener(principal.getName()));
        Aviso aviso = Aviso.de(r, "Movimiento eliminado", "Acción no permitida");
        avisos.addFlashAttribute("aviso", r.exito() ? aviso.conSonido("borrar") : aviso);
        return "redirect:/panel?pestana=movimientos";
    }

    @PostMapping("/panel/presupuesto")
    public String presupuesto(@RequestParam(defaultValue = "") String monto, Principal principal, RedirectAttributes avisos) {
        Resultado r = finanzas.guardarPresupuesto(usuarios.obtener(principal.getName()), monto);
        avisos.addFlashAttribute("aviso", Aviso.de(r, "Presupuesto", "Validación"));
        return "redirect:/panel?pestana=inicio";
    }

    // ===================== Gestión de Usuarios (SOLO ADMIN) =====================

    /** Revisa en la base de datos (no solo en la sesión) que siga siendo ADMIN. */
    private boolean esAdmin(Principal principal) {
        return usuarios.buscar(principal.getName()).map(u -> u.getRol() == Rol.ADMIN && u.estaActivo()).orElse(false);
    }

    private String sinPermiso(RedirectAttributes avisos) {
        avisos.addFlashAttribute("aviso", Aviso.advertencia("Acción no permitida", "Solo un administrador puede gestionar usuarios."));
        return "redirect:/panel";
    }

    @PostMapping("/panel/usuarios")
    public String crearUsuario(@RequestParam(defaultValue = "") String usuario,
                               @RequestParam(defaultValue = "") String clave,
                               @RequestParam(defaultValue = "") String nombreCompleto,
                               @RequestParam(defaultValue = "CONSULTA") Rol rol,
                               Principal principal, RedirectAttributes avisos) {
        if (!esAdmin(principal)) return sinPermiso(avisos);
        Resultado r = usuarios.crearUsuario(usuario, clave, nombreCompleto, rol);
        avisos.addFlashAttribute("aviso", Aviso.de(r, "Usuario creado", "Error"));
        return "redirect:/panel?pestana=usuarios";
    }

    @PostMapping("/panel/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam(required = false) Long id, Principal principal, RedirectAttributes avisos) {
        if (!esAdmin(principal)) return sinPermiso(avisos);
        Resultado r = id == null
            ? Resultado.error("Seleccione primero un usuario de la tabla.")
            : usuarios.eliminar(id, principal.getName());
        avisos.addFlashAttribute("aviso", r.exito()
            ? Aviso.informacion("Usuario eliminado", r.mensaje()).conSonido("borrar")
            : Aviso.advertencia("Acción no permitida", r.mensaje()));
        return "redirect:/panel?pestana=usuarios";
    }

    @PostMapping("/panel/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam Rol rol, Principal principal, RedirectAttributes avisos) {
        if (!esAdmin(principal)) return sinPermiso(avisos);
        Resultado r = usuarios.cambiarRol(id, rol, principal.getName());
        avisos.addFlashAttribute("aviso", Aviso.de(r, "Rol actualizado", "Acción no permitida"));
        return "redirect:/panel?pestana=usuarios";
    }

    @PostMapping("/panel/usuarios/{id}/aprobar")
    public String aprobar(@PathVariable Long id, @RequestParam(defaultValue = "CONSULTA") Rol rol,
                          Principal principal, RedirectAttributes avisos) {
        if (!esAdmin(principal)) return sinPermiso(avisos);
        avisos.addFlashAttribute("aviso", Aviso.de(usuarios.aprobar(id, rol), "Cuenta aprobada", "Error"));
        return "redirect:/panel?pestana=usuarios";
    }

    @PostMapping("/panel/usuarios/{id}/desbloquear")
    public String desbloquear(@PathVariable Long id, Principal principal, RedirectAttributes avisos) {
        if (!esAdmin(principal)) return sinPermiso(avisos);
        avisos.addFlashAttribute("aviso", Aviso.de(usuarios.desbloquear(id), "Usuario desbloqueado", "Error"));
        return "redirect:/panel?pestana=usuarios";
    }

    // ===================== Mi cuenta (todos) =====================

    @PostMapping("/panel/cuenta/clave")
    public String cambiarClave(@RequestParam(defaultValue = "") String actual,
                               @RequestParam(defaultValue = "") String nueva,
                               @RequestParam(defaultValue = "") String confirmar,
                               Principal principal, RedirectAttributes avisos) {
        Resultado r = usuarios.cambiarClave(principal.getName(), actual, nueva, confirmar);
        avisos.addFlashAttribute("aviso", Aviso.de(r, "Contraseña actualizada", "Validación"));
        return "redirect:/panel?pestana=cuenta";
    }

    @PostMapping("/panel/cuenta/sonido")
    public String sonido(@RequestParam(defaultValue = "false") boolean activo, Principal principal, RedirectAttributes avisos) {
        usuarios.cambiarSonido(principal.getName(), activo);
        avisos.addFlashAttribute("aviso", Aviso.informacion("Sonidos", activo ? "Sonidos activados." : "Sonidos desactivados.")
            .conSonido(activo ? "ingreso" : ""));
        return "redirect:/panel?pestana=cuenta";
    }

    @PostMapping("/panel/resumen/visto")
    public String resumenVisto(@RequestParam String mes, @RequestParam(defaultValue = "false") boolean abrir, Principal principal) {
        usuarios.marcarResumenVisto(principal.getName(), mes);
        return abrir ? "redirect:/panel?pestana=graficas&mes=" + mes : "redirect:/panel";
    }
}
