package com.moneyos.seguridad;

import com.moneyos.servicio.UsuarioServicio;
import com.moneyos.web.Aviso;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Qué hacer cuando el usuario o la contraseña no coinciden (la rama "else" del botón Ingresar). */
@Component
public class ManejadorLoginFallido implements AuthenticationFailureHandler {

    public static final String AVISO = "moneyos.avisoLogin";
    public static final String ULTIMO_USUARIO = "moneyos.ultimoUsuario";

    private final UsuarioServicio usuarios;

    public ManejadorLoginFallido(UsuarioServicio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException excepcion) throws IOException {
        HttpSession sesion = request.getSession();
        String usuario = request.getParameter("username") == null ? "" : request.getParameter("username").trim();
        String clave = request.getParameter("password") == null ? "" : request.getParameter("password").trim();
        sesion.setAttribute(ULTIMO_USUARIO, usuario);

        Aviso aviso;
        if (usuario.isEmpty() || clave.isEmpty()) {
            // 1) Campos vacíos: no cuenta como intento
            aviso = Aviso.advertencia("Campos obligatorios", "Debe escribir su usuario y su contraseña.");
        } else if (excepcion instanceof DisabledException) {
            aviso = Aviso.advertencia("Cuenta pendiente",
                "Su cuenta todavía no ha sido aprobada por un administrador.");
        } else if (excepcion instanceof LockedException) {
            aviso = Aviso.error("Acceso bloqueado",
                "Esta cuenta está bloqueada por seguridad durante " + UsuarioServicio.MINUTOS_BLOQUEO
                    + " minutos por intentos fallidos.\nIntente más tarde o pida al administrador que la desbloquee.");
        } else {
            // 2) Usuario o contraseña incorrectos: se suma un intento (en la ventana y en la cuenta)
            usuarios.registrarIntentoFallido(usuario);
            int restantes = IntentosLogin.sumarFallo(sesion);
            if (restantes > 0) {
                aviso = Aviso.error("Error de autenticación",
                    "Usuario o contraseña incorrectos.\nLe quedan " + restantes + " intento(s).");
            } else {
                aviso = Aviso.error("Acceso bloqueado",
                    "Ha superado el límite de 3 intentos fallidos.\nEl acceso ha sido bloqueado por seguridad.");
            }
        }
        sesion.setAttribute(AVISO, aviso);
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
