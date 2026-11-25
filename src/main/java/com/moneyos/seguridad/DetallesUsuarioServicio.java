package com.moneyos.seguridad;

import com.moneyos.modelo.Usuario;
import com.moneyos.repositorio.UsuarioRepositorio;
import com.moneyos.util.Colombia;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Le dice a Spring Security cómo encontrar un usuario (el "validarLogin" de la guía).
 * Spring compara la contraseña cifrada y revisa si la cuenta está bloqueada o pendiente.
 */
@Service
public class DetallesUsuarioServicio implements UserDetailsService {

    private final UsuarioRepositorio usuarios;

    public DetallesUsuarioServicio(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) throws UsernameNotFoundException {
        // El usuario ignora mayúsculas/minúsculas (como equalsIgnoreCase en la guía);
        // la contraseña sí es exacta.
        Usuario u = usuarios.findByUsuarioIgnoreCase(usuario == null ? "" : usuario.trim())
            .orElseThrow(() -> new UsernameNotFoundException("No existe"));

        return User.withUsername(u.getUsuario())
            .password(u.getClave())
            .roles(u.getRol().name())
            .accountLocked(u.estaBloqueado(Colombia.ahora()))
            .disabled(!u.estaActivo())
            .build();
    }
}
