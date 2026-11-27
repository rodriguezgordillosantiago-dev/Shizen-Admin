package com.shizen.Admin.controller;

import com.shizen.Admin.model.Mensaje;
import com.shizen.Admin.service.TicketService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** Coordina las rutas de la bandeja de soporte y sus conversaciones en vivo. */
@Controller
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** Carga los tickets recientes y el identificador del administrador conectado. */
    @GetMapping({"/soporte", "/soporte.html"})
    public String listarTickets(Model model, Principal principal) {
        model.addAttribute("tickets", ticketService.listarTickets());

        if (principal != null) {
            ticketService.obtenerAdminId(principal.getName())
                    .ifPresent(adminId -> model.addAttribute("adminId", adminId));
        }
        return "soporte";
    }

    /** Crea un ticket y su conversación de soporte asociada. */
    @PostMapping("/soporte/crear")
    public String crearTicket(
            @RequestParam String asunto,
            @RequestParam String descripcion,
            @RequestParam(required = false) Integer idPedido,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        ticketService.crearTicket(
                asunto,
                descripcion,
                idPedido,
                principal != null ? principal.getName() : null);
        redirectAttributes.addFlashAttribute("ticketCreado", true);
        redirectAttributes.addFlashAttribute("ticketAsunto", asunto);
        return "redirect:/soporte";
    }

    /** Cierra el ticket y guarda la fecha de cierre. */
    @PostMapping("/soporte/{id}/cerrar")
    public String cerrarTicket(@PathVariable Integer id) {
        ticketService.cerrarTicket(id);
        return "redirect:/soporte";
    }

    /** Devuelve los mensajes de la conversación en orden de envío para la interfaz. */
    @GetMapping("/soporte/{id}/mensajes")
    @ResponseBody
    public List<Mensaje> obtenerMensajes(@PathVariable Integer id) {
        return ticketService.obtenerMensajes(id);
    }

    /** Guarda el mensaje recibido por STOMP y lo publica a quienes siguen el ticket. */
    @MessageMapping("/chat/{idTicket}")
    public void enviarMensajeWebSocket(
            @DestinationVariable Integer idTicket,
            Map<String, String> payload,
            Principal principal) {
        ticketService.enviarMensajeWebSocket(
                idTicket,
                payload.get("contenido"),
                principal != null ? principal.getName() : null);
    }
}

