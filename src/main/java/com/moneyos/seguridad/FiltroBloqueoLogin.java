package com.moneyos.seguridad;

import com.moneyos.web.Aviso;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Si esta ventana de login ya agotó sus 3 intentos, ni siquiera revisa la contraseña:
 * es el equivalente a deshabilitar los campos y botones en la guía.
 */
@Component
public class FiltroBloqueoLogin extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
        throws ServletException, IOException {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        boolean esIngreso = "POST".equals(request.getMethod()) && "/login".equals(ruta);
        HttpSession sesion = request.getSession(false);
        if (esIngreso && sesion != null && IntentosLogin.bloqueoRestante(sesion) > 0) {
            sesion.setAttribute(ManejadorLoginFallido.AVISO, Aviso.error("Acceso bloqueado",
                "Ha superado el límite de 3 intentos fallidos.\nEl acceso ha sido bloqueado por seguridad."));
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        cadena.doFilter(request, response);
    }
}
