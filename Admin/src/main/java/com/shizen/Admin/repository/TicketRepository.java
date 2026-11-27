package com.shizen.Admin.repository;

import com.shizen.Admin.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/** Consultas usadas para cargar la bandeja de soporte. */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /** Obtiene la bandeja completa, del ticket más reciente al más antiguo. */
    List<Ticket> findAllByOrderByFechaCreacionDesc();
}
