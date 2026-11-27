package com.shizen.Admin.repository;


import com.shizen.Admin.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Consulta los mensajes guardados de las conversaciones de soporte. */
public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {
    /** Devuelve el historial de una conversación desde el mensaje más antiguo. */
    List<Mensaje>
    findByIdConversacionOrderByFechaEnvioAsc(Integer idConversacion);
}