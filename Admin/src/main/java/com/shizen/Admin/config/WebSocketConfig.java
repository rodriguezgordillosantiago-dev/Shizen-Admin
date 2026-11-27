package com.shizen.Admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** Configura la conexión STOMP usada para actualizar el chat de soporte en vivo. */
@Configuration 
@EnableWebSocketMessageBroker //Activa el servidor del websocker con el protocolo alternativo de STOMP
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

@Override
public void configureMessageBroker(MessageBrokerRegistry config) {

 // Habilita un broker simple en memoria para los canales que empiezan con /topic
 // Por aquí es por donde el servidor "publica" mensajes a los clientes

 config.enableSimpleBroker("/topic");

 // Prefijo para los mensajes que van desde el cliente hacia el servidor (@MessageMapping)

 config.setApplicationDestinationPrefixes("/app");
}

   @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // La URL a la que el navegador se conecta inicialmente
        // withSockJS() permite soporte si el navegador no tiene WebSocket puro
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}


