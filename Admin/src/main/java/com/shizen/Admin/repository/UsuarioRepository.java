package com.shizen.Admin.repository;

import com.shizen.Admin.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository 

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Spring Data JPA crea automáticamente la consulta SQL:
    // SELECT * FROM usuarios WHERE email = ?

    Optional<Usuario> findByEmail(String email);
    
}