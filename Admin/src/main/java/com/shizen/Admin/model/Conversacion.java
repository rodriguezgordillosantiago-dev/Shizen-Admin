package com.shizen.Admin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "conversacion")
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Integer idConversacion;

    @Column (length = 20, nullable = false)
    private String tipo; //Soporte o directo

    @Column(name = "id_ticket", unique = true)
    private Integer idTicket;

    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column (name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public Conversacion() { 
    }

    //Getters y setters
    public Integer getIdTicket(){
        return idTicket;
    }

    public void setIdTicket(Integer idTicket) {
        this.idTicket = idTicket;
    }

    public Integer getIdConversacion() {
        return idConversacion;
    }

    public void setIdConversacion(Integer idConversacion) {
        this.idConversacion = idConversacion;
    }

    public String getTipo() { 
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) { 
        this.fechaCreacion = fechaCreacion;
    }


}
