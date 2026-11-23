package com.shizen.Admin.repository;

import com.shizen.Admin.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
public interface UsuariosRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findAllByOrderByFechaRegistroDesc();

    // Búsqueda inteligente por coincidencia en nombre, apellido o correo, y por rol
    @Query("SELECT u FROM Usuario u WHERE " +
           "(:rol IS NULL OR :rol = '' OR LOWER(u.rol) = LOWER(:rol)) AND " +
           "(:buscar IS NULL OR :buscar = '' OR " +
           " LOWER(u.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
           " LOWER(u.apellido) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
           "ORDER BY u.fechaRegistro DESC")
    List<Usuario> buscarConFiltros(@Param("buscar") String buscar, @Param("rol") String rol);
}