package com.moneyos.seguridad;

import com.moneyos.modelo.Usuario;
import com.moneyos.servicio.UsuarioServicio;
import com.moneyos.web.Aviso;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Acceso concedido: saluda, reinicia los intentos y abre el Panel Administrativo. */
@Component
public class ManejadorLoginExitoso implements AuthenticationSuccessHandler {

    public static final String AVISO_PANEL = "moneyos.avisoPanel";

    private final UsuarioServicio usuarios;

    public ManejadorLoginExitoso(UsuarioServicio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication autenticacion) throws IOException {
        HttpSession sesion = request.getSession();
        IntentosLogin.reiniciar(sesion);
        sesion.removeAttribute(ManejadorLoginFallido.ULTIMO_USUARIO);

        usuarios.registrarIngreso(autenticacion.getName());
        Usuario u = usuarios.obtener(autenticacion.getName());
        sesion.setAttribute(AVISO_PANEL, Aviso.informacion("Acceso concedido",
            "¡Bienvenido(a), " + u.getNombreCompleto() + "!\nRol asignado: " + u.getRol()));

        response.sendRedirect(request.getContextPath() + "/panel");
    }
}
