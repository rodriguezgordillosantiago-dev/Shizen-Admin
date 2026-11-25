package com.shizen.Admin.controller;

import com.shizen.Admin.repository.UsuarioRepository;
import com.shizen.Admin.model.Ticket;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.shizen.Admin.repository.TicketRepository;
import com.shizen.Admin.model.Conversacion;
import com.shizen.Admin.model.Mensaje;
import com.shizen.Admin.repository.ConversacionRepository;
import com.shizen.Admin.repository.MensajeRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.security.Principal;


@Controller 
public class TicketController {
    
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TicketController(TicketRepository ticketRepository, UsuarioRepository usuarioRepository, ConversacionRepository conversacionRepository, MensajeRepository mensajeRepository, SimpMessagingTemplate messagingTemplate) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.messagingTemplate = messagingTemplate;
        

    }



//Listar los tickects en la bandeja de entrada
@GetMapping ({"/soporte", "/soporte.html"})
public String listarTickets(Model model, Principal principal)
{
      List<Ticket> lista = ticketRepository.findAllByOrderByFechaCreacionDesc();
    model.addAttribute("tickets", lista);

    if (principal != null) {
        usuarioRepository.findByEmail(principal.getName()).ifPresent( u -> {

            model.addAttribute("adminId", u.getIdUsuario());
        });
    }
    return "soporte";
}

    @PostMapping("/soporte/crear")
    public String crearTicket(@RequestParam String asunto, @RequestParam String descripcion, @RequestParam(required = false) Integer idPedido, Principal principal, RedirectAttributes redirectAttributes) {
        Ticket t = new Ticket();
        t.setAsunto(asunto);
        t.setDescripcion(descripcion);
        t.setIdPedido(idPedido);
        t.setEstado("abierto");
        t.setFechaCreacion(LocalDateTime.now());

        if (principal != null) {
            usuarioRepository.findByEmail(principal.getName()).ifPresent(u -> {
                t.setIdUsuario(u.getIdUsuario());
            });
        }


        ticketRepository.save(t);

            // Crear conversacion ligada al ticket de ayuda
    Conversacion c = new Conversacion();
        c.setTipo("SOPORTE");
        c.setIdTicket(t.getIdTicket());
        c.setFechaCreacion(LocalDateTime.now());
        conversacionRepository.save(c);    
        redirectAttributes.addFlashAttribute("ticketCreado", true);
        redirectAttributes.addFlashAttribute("ticketAsunto", t.getAsunto());
        return "redirect:/soporte";
    
    }




    // Cerrar / Resolver Ticket
    @PostMapping("/soporte/{id}/cerrar")
    public String cerrarTicket(@PathVariable Integer id) {
        Ticket t = ticketRepository.findById(id).orElseThrow();
        t.setEstado("cerrado");
        t.setFechaCierre(LocalDateTime.now());
        ticketRepository.save(t);
        return "redirect:/soporte";
    }

    //Cargar los mensajes de un ticket de ayuda (devuelve JSON para AJAX)
    
    @GetMapping ("/soporte/{id}/mensajes")
    @ResponseBody 
    public List<Mensaje>
    obtenerMensajes(@PathVariable Integer id) {
        Conversacion conv = conversacionRepository.findByIdTicket(id).orElse(null);
        if (conv == null) return List.of();
        return mensajeRepository.findByIdConversacionOrderByFechaEnvioAsc(conv.getIdConversacion());
        
    }

    //Enviar mensaje del ticket
    @PostMapping ("/soporte/{id}/responder")

    public String responderTicket(@PathVariable Integer id, @RequestParam String contenido, Principal principal) {

        Conversacion conv = conversacionRepository.findByIdTicket(id).orElseThrow();

        Mensaje m = new Mensaje();

        m.setIdConversacion(conv.getIdConversacion());
        m.setContenido(contenido);
        m.setFechaEnvio(LocalDateTime.now());

        if (principal != null) {

            usuarioRepository.findByEmail(principal.getName()).ifPresent(u -> {
                m.setIdRemitente(u.getIdUsuario());
            });
        }
        mensajeRepository.save(m);
        return "redirect:/soporte";
    }

    @MessageMapping 
    ("/chat/{idTicket}")
    
    public void 
    enviarMensajeWebSocket(@DestinationVariable Integer idTicket, Map<String, String> payload,

    Principal principal) {

        Conversacion conv = conversacionRepository.findByIdTicket(idTicket).orElseThrow();

        Mensaje m = new Mensaje();

        m.setIdConversacion(conv.getIdConversacion());
        m.setContenido(payload.get("contenido"));
        m.setFechaEnvio(LocalDateTime.now());

        if (principal != null) {
            usuarioRepository.findByEmail(principal.getName()).ifPresent(u-> {

                m.setIdRemitente(u.getIdUsuario());
            });
        }
    Mensaje guardado = mensajeRepository.save(m);


    //Asignar un admin al ticket si no lo tiene

    Ticket ticket = ticketRepository.findById(idTicket).orElse(null);
    if (ticket != null && ticket.getIdAdminAsignado() == null && m.getIdRemitente() != null) {
        ticket.setIdAdminAsignado(m.getIdRemitente());
        ticketRepository.save(ticket);
    }

    //retransmitir / hacer que todos los conectados al chat de ticket "Canal" puedan verlo en vivo

    messagingTemplate.convertAndSend("/topic/ticket/" + idTicket, guardado);

}
}

