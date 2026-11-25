package com.moneyos.repositorio;

import com.moneyos.modelo.EstadoUsuario;
import com.moneyos.modelo.Rol;
import com.moneyos.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);

    List<Usuario> findAllByOrderByCreadoEnAsc();

    long countByRolAndEstado(Rol rol, EstadoUsuario estado);

    long countByEstado(EstadoUsuario estado);
}
