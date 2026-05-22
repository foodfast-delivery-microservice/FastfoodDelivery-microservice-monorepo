package com.example.notificationservice.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket and STOMP broker configuration.
 * Sets up message prefix routing, user destination mapping, and registers connection interceptors.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:5173,http://localhost:3000,http://localhost:5174}")
    private String allowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register connection endpoint and allow cross-origin requests
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns(allowedOriginPatterns.split(","))
                .withSockJS();
        
        // Add plain WebSocket support without SockJS as fallback
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns(allowedOriginPatterns.split(","));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple broker for routing messages to users
        registry.enableSimpleBroker("/user/queue");
        
        // Prefix for client-to-server destination mappings
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix for mapping user-specific destination queues (/user/queue/notifications)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Wire JWT authentication interceptor into incoming client requests
        registration.interceptors(webSocketAuthInterceptor);
    }
}
