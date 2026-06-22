package com.guang.miniecommercebackend.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization on WebSocket CONNECT");
        }

        String token = auth.substring(7);
        if (!jwtService.isValid(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        String username = jwtService.parseClaims(token).getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = jwtService.parseClaims(token).get("roles", List.class);

        var authorities = roles.stream()
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .toList();

        accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, authorities));
        return message;
    }
}
