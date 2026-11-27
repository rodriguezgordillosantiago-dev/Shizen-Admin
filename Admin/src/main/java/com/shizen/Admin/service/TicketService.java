package com.shizen.Admin.service;

import com.shizen.Admin.model.Conversacion;
import com.shizen.Admin.model.Mensaje;
import com.shizen.Admin.model.Ticket;
import com.shizen.Admin.repository.ConversacionRepository;
import com.shizen.Admin.repository.MensajeRepository;
import com.shizen.Admin.repository.TicketRepository;
import com.shizen.Admin.repository.UsuarioRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Agrupa las operaciones de negocio y persistencia de los tickets de soporte. */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TicketService(
            TicketRepository ticketRepository,
            UsuarioRepository usuarioRepository,
            ConversacionRepository conversacionRepository,
            MensajeRepository mensajeRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /** Devuelve la bandeja desde el ticket más reciente. */
    @Transactional(readOnly = true)
    public List<Ticket> listarTickets() {
        return ticketRepository.findAllByOrderByFechaCreacionDesc();
    }

    /** Busca el identificador de usuario asociado al correo conectado. */
    @Transactional(readOnly = true)
    public Optional<Integer> obtenerAdminId(String email) {
        return usuarioRepository.findByEmail(email).map(usuario -> usuario.getIdUsuario());
    }

    /** Crea el ticket y su conversación en una misma transacción. */
    @Transactional
    public void crearTicket(String asunto, String descripcion, Integer idPedido, String email) {
        Ticket ticket = new Ticket();
        ticket.setAsunto(asunto);
        ticket.setDescripcion(descripcion);
        ticket.setIdPedido(idPedido);
        ticket.setEstado("abierto");
        ticket.setFechaCreacion(LocalDateTime.now());

        if (email != null) {
            usuarioRepository.findByEmail(email)
                    .ifPresent(usuario -> ticket.setIdUsuario(usuario.getIdUsuario()));
        }

        ticketRepository.save(ticket);

        Conversacion conversacion = new Conversacion();
        conversacion.setTipo("SOPORTE");
        conversacion.setIdTicket(ticket.getIdTicket());
        conversacion.setFechaCreacion(LocalDateTime.now());
        conversacionRepository.save(conversacion);
    }

    /** Cierra un ticket y registra cuándo se cerró. */
    @Transactional
    public void cerrarTicket(Integer id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        ticket.setEstado("cerrado");
        ticket.setFechaCierre(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    /** Devuelve los mensajes de la conversación en orden de envío. */
    @Transactional(readOnly = true)
    public List<Mensaje> obtenerMensajes(Integer idTicket) {
        return conversacionRepository.findByIdTicket(idTicket)
                .map(conversacion -> mensajeRepository.findByIdConversacionOrderByFechaEnvioAsc(
                        conversacion.getIdConversacion()))
                .orElseGet(List::of);
    }

    /** Guarda el mensaje STOMP, asigna al primer administrador y publica la actualización. */
    @Transactional
    public void enviarMensajeWebSocket(Integer idTicket, String contenido, String email) {
        Conversacion conversacion = conversacionRepository.findByIdTicket(idTicket).orElseThrow();

        Mensaje mensaje = new Mensaje();
        mensaje.setIdConversacion(conversacion.getIdConversacion());
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(LocalDateTime.now());

        if (email != null) {
            usuarioRepository.findByEmail(email)
                    .ifPresent(usuario -> mensaje.setIdRemitente(usuario.getIdUsuario()));
        }

        Mensaje guardado = mensajeRepository.save(mensaje);

        ticketRepository.findById(idTicket).ifPresent(ticket -> {
            if (ticket.getIdAdminAsignado() == null && mensaje.getIdRemitente() != null) {
                ticket.setIdAdminAsignado(mensaje.getIdRemitente());
                ticketRepository.save(ticket);
            }
        });

        messagingTemplate.convertAndSend("/topic/ticket/" + idTicket, guardado);
    }
}
