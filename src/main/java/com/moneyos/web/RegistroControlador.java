package com.moneyos.web;

import com.moneyos.seguridad.ManejadorLoginFallido;
import com.moneyos.servicio.Resultado;
import com.moneyos.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** "Crear cuenta": registro público de usuarios (queda pendiente hasta que el ADMIN apruebe). */
@Controller
public class RegistroControlador {

    private final UsuarioServicio usuarios;

    public RegistroControlador(UsuarioServicio usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/registro")
    public String formulario() {
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam(defaultValue = "") String nombreCompleto,
                            @RequestParam(defaultValue = "") String usuario,
                            @RequestParam(defaultValue = "") String correo,
                            @RequestParam(defaultValue = "") String celular,
                            @RequestParam(defaultValue = "") String clave,
                            @RequestParam(defaultValue = "") String confirmar,
                            @RequestParam(defaultValue = "false") boolean aceptaDatos,
                            HttpSession sesion, Model modelo) {
        Resultado r = usuarios.registrar(nombreCompleto, usuario, correo, celular, clave, confirmar, aceptaDatos);
        if (r.exito()) {
            sesion.setAttribute(ManejadorLoginFallido.AVISO, Aviso.informacion("Cuenta creada", r.mensaje()));
            return "redirect:/login";
        }
        // Volver a mostrar el formulario con lo que escribió (menos las contraseñas)
        modelo.addAttribute("aviso", Aviso.advertencia("Revise el formulario", r.mensaje()));
        modelo.addAttribute("nombreCompleto", nombreCompleto);
        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("correo", correo);
        modelo.addAttribute("celular", celular);
        modelo.addAttribute("aceptaDatos", aceptaDatos);
        return "registro";
    }
}
