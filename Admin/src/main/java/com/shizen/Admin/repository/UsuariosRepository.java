package com.shizen.Admin.repository;

import com.shizen.Admin.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuariosRepository extends JpaRepository<Usuario, Integer>, UsuariosRepositoryCustom {

    List<Usuario> findAllByOrderByFechaRegistroDesc();
}
