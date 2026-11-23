package com.shizen.Admin.repository;


import com.shizen.Admin.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {
    List<Mensaje>
    findByIdConversacionOrderByFechaEnvioAsc(Integer idConversacion);
}