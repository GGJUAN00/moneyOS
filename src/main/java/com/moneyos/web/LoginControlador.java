package com.moneyos.web;

import com.moneyos.seguridad.IntentosLogin;
import com.moneyos.seguridad.ManejadorLoginFallido;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** La "puerta de entrada" (VentanaLogin de la guía). */
@Controller
public class LoginControlador {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/panel";
    }

    @GetMapping("/salud")
    @ResponseBody
    public String salud() {
        return "ok";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String salir, Authentication autenticacion,
                        HttpSession sesion, Model modelo) {
        // Si ya inició sesión, va directo al panel
        if (autenticacion != null && autenticacion.isAuthenticated()
            && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            return "redirect:/panel";
        }

        Object aviso = sesion.getAttribute(ManejadorLoginFallido.AVISO);
        sesion.removeAttribute(ManejadorLoginFallido.AVISO);
        if (aviso == null && salir != null) {
            aviso = Aviso.informacion("Sesión cerrada", "Su sesión se cerró correctamente.");
        }

        long bloqueoMs = IntentosLogin.bloqueoRestante(sesion);
        modelo.addAttribute("aviso", aviso);
        modelo.addAttribute("intentosDisponibles", bloqueoMs > 0 ? 0 : IntentosLogin.disponibles(sesion));
        modelo.addAttribute("bloqueado", bloqueoMs > 0);
        modelo.addAttribute("segundosBloqueo", (bloqueoMs + 999) / 1000);
        modelo.addAttribute("ultimoUsuario", sesion.getAttribute(ManejadorLoginFallido.ULTIMO_USUARIO));
        return "login";
    }
}
