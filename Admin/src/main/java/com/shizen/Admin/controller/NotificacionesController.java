package com.shizen.Admin.controller;

import com.shizen.Admin.model.Notificacion;
import com.shizen.Admin.repository.NotificacionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class NotificacionesController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionesController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping({"/notificaciones", "/notificaciones.html"})
    public String listarNotificaciones(Model model) {
        List<Notificacion> lista = notificacionRepository.findAllByOrderByFechaCreacionDesc();
        model.addAttribute("notificaciones", lista);
        return "notificaciones";
    }
}