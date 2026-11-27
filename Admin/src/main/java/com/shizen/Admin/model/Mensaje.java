package com.shizen.Admin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Representa un mensaje guardado dentro de una conversación. */
@Entity
@Table(name = "mensaje")
public class Mensaje {

 @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @Column(name = "id_conversacion", nullable = false)
    private Integer idConversacion;

    @Column(name = "id_remitente")
    private Integer idRemitente;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    public Mensaje (){
    }

    public Integer getIdMensaje(){
        return idMensaje;
    }

    public void setIdMensaje(Integer idMensaje) { 
        this.idMensaje = idMensaje;
    }

    public Integer getIdConversacion() { 
        return idConversacion;
    }

    public void setIdConversacion(Integer idConversacion) {
        this.idConversacion = idConversacion;
    }

    public Integer getIdRemitente(){
        return idRemitente;
    }

    public void setIdRemitente(Integer idRemitente) {
        this.idRemitente = idRemitente;
    }

    public String getContenido(){
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaEnvio(){
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio){
        this.fechaEnvio = fechaEnvio;
    }
}