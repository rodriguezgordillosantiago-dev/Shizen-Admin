package com.shizen.Admin.repository;

import com.shizen.Admin.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository 
public interface NotificacionRepository
extends JpaRepository<Notificacion, Integer>
{
    List<Notificacion> findAllByAudienciaOrderByFechaCreacionDesc(String audiencia);
}