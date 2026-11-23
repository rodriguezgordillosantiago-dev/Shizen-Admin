package com.shizen.Admin.repository;

import com.shizen.Admin.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversacionRepository 

    extends JpaRepository<Conversacion, Integer>
    {
        Optional<Conversacion>
        findByIdTicket(Integer idTicket);
    }
