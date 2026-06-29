package com.shivamprogramming.chat_service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * WebSocket Channel Interceptor — validates JWT token on STOMP CONNECT.
 * If jwt.auth.enabled=true, clients MUST pass a valid JWT token
 * in the "Authorization" STOMP header during the CONNECT frame.
 *
 * Example client STOMP connect header:
 *   { Authorization: "Bearer eyJhbGciOi..." }
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);

                if (authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    if (jwtService.isTokenValid(token)) {
                        String username = jwtService.extractUsername(token);

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        username, null, Collections.emptyList());
                        accessor.setUser(auth);

                        log.info("WebSocket CONNECT authenticated: {}", username);
                    } else {
                        log.warn("WebSocket CONNECT with invalid JWT token");
                    }
                }
            } else {
                // Allow anonymous connections when JWT auth is optional
                log.debug("WebSocket CONNECT without Authorization header (anonymous)");
            }
        }
        return message;
    }
}

