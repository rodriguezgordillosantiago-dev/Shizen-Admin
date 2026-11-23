package com.shizen.Admin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "ticket")
public class Ticket {

    @Id @GeneratedValue (strategy =  GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Integer idTicket;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(length = 150)
    private String asunto;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 20)
    private String estado;

    @Column(name = "id_admin_asignado")
    private Integer idAdminAsignado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    public Ticket() {
    }

    public Integer getIdTicket() {
        return idTicket;
    }

    public void setIdTicket(Integer idTicket) {
        this.idTicket = idTicket;
    }

    public Integer getIdUsuario() { 
        return idUsuario;
        
    }

    public void setIdUsuario(Integer idUsuario){
        this.idUsuario = idUsuario;
    }

        public Integer getIdPedido() { 
        return idPedido;
        
    }

    public void setIdPedido(Integer idPedido){
        this.idPedido = idPedido;
    }

    public String getAsunto(){
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getDescripcion() { 
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado(){
        return estado;
    }

    public void setEstado(String estado) { 
        this.estado = estado;
    }

    public Integer getIdAdminAsignado() {
    return idAdminAsignado;
    }
    
    public void setIdAdminAsignado(Integer idAdminAsignado) {
    this.idAdminAsignado = idAdminAsignado;
    }

    public LocalDateTime getFechaCreacion(){
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion){
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaCierre(){
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre){
        this.fechaCierre = fechaCierre;
    }

    
}