package com.moneyos.repositorio;

import com.moneyos.modelo.Movimiento;
import com.moneyos.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovimientoRepositorio extends JpaRepository<Movimiento, Long> {

    /** Todos, del más reciente al más antiguo. */
    @Query("select m from Movimiento m left join fetch m.registradoPor order by m.fecha desc, m.creadoEn desc")
    List<Movimiento> findAllRecientes();

    /** Al eliminar un usuario, sus movimientos se conservan sin autor. */
    @Modifying
    @Query("update Movimiento m set m.registradoPor = null where m.registradoPor = :usuario")
    void quitarAutor(Usuario usuario);
}
