package com.shizen.Admin.repository;

import com.shizen.Admin.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository 
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

        // Devuelve todos los tickets ordenados por fecha de creación descendente

        List<Ticket> findAllByOrderByFechaCreacionDesc();

        //Filtro por estado

        List<Ticket> findByEstadoOrderByFechaCreacionDesc(String estado);
}
