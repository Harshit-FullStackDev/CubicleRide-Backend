package com.orangemantra.rideservice.config;

import com.orangemantra.rideservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class StompJwtAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Claims claims = jwtUtil.extractAllClaims(token);
                    String empId = claims.get("empId", String.class);
                    if (empId != null && !empId.isBlank()) {
                        UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(empId, null, Collections.emptyList());
                        accessor.setUser(user);
                    }
                } catch (Exception ignored) {}
            }
        }
        return message;
    }
}

