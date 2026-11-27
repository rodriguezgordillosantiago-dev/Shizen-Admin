package com.shizen.Admin.repository;

import com.shizen.Admin.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Busca la conversación de soporte asociada a un ticket. */
public interface ConversacionRepository 

    extends JpaRepository<Conversacion, Integer>
    {
        /** Devuelve la conversación vinculada al identificador del ticket. */
        Optional<Conversacion>
        findByIdTicket(Integer idTicket);
    }
