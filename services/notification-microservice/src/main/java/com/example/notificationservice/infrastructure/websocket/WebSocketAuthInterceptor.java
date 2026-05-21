package com.example.notificationservice.infrastructure.websocket;

import com.example.notificationservice.infrastructure.security.JwtGrantedAuthoritiesConverter;
import com.example.notificationservice.infrastructure.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor to authenticate WebSocket connections during STOMP CONNECT.
 * Extracts JWT from Authorization header, decodes and converts it to authentication token.
 * Customizes Principal name to be the numeric userId for SimpMessagingTemplate user destination routing.
 * Implements IP and User rate limiting to defend against connection exhaustion/denial of service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final org.springframework.beans.factory.ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final JwtGrantedAuthoritiesConverter authoritiesConverter;
    private final JwtTokenService jwtTokenService;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    // Rate Limiting Map
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();

    private static class RateLimitInfo {
        final long windowStart;
        final AtomicInteger count;

        RateLimitInfo(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }

    private boolean isRateLimited(String key, int maxRequests, long windowSizeMs) {
        long now = System.currentTimeMillis();
        
        // Self-cleaning to prevent memory leaks when map grows
        if (rateLimits.size() > 1000) {
            long cutoff = now - 60000; // clean up entries older than 1 minute
            rateLimits.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
        }

        RateLimitInfo info = rateLimits.compute(key, (k, v) -> {
            if (v == null || (now - v.windowStart) > windowSizeMs) {
                return new RateLimitInfo(now, 1);
            } else {
                v.count.incrementAndGet();
                return v;
            }
        });
        return info.count.get() > maxRequests;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 1. IP-Based Connection Rate Limiting
            String clientIp = accessor.getFirstNativeHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.trim().isEmpty()) {
                clientIp = accessor.getFirstNativeHeader("X-Real-IP");
            }
            if (clientIp == null || clientIp.trim().isEmpty()) {
                clientIp = "unknown";
            }

            if (!"unknown".equals(clientIp)) {
                // Max 10 connection attempts per 10 seconds per IP
                if (isRateLimited("ip:" + clientIp, 10, 10000)) {
                    log.warn("✗ WebSocket connection rejected: Rate limit exceeded for IP: {}", clientIp);
                    throw new IllegalArgumentException("Connection rate limit exceeded. Please try again later.");
                }
            }

            if (!securityEnabled) {
                log.info("Security is disabled. Setting dummy authentication with userId 1 for WebSocket connection.");
                accessor.setUser(new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList()));
                return message;
            }

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.info("WebSocket CONNECT attempt. Authorization header present: {}", authHeader != null);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
                    if (jwtDecoder == null) {
                        throw new IllegalStateException("JwtDecoder bean is not available even though security is enabled.");
                    }
                    Jwt jwt = jwtDecoder.decode(token);
                    Long userId = jwtTokenService.extractUserId(jwt);

                    // 2. User-Based Connection Rate Limiting
                    // Max 5 connection attempts per 10 seconds per authenticated user
                    if (isRateLimited("user:" + userId, 5, 10000)) {
                        log.warn("✗ WebSocket connection rejected: Rate limit exceeded for userId: {}", userId);
                        throw new IllegalArgumentException("Connection rate limit exceeded for this user. Please try again later.");
                    }

                    Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
                    
                    // Set Principal name to userId so convertAndSendToUser works with userId
                    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities, String.valueOf(userId));
                    accessor.setUser(auth);
                    log.info("✓ WebSocket connection authenticated successfully for userId: {}", userId);
                } catch (Exception e) {
                    log.error("✗ WebSocket JWT Authentication failed", e);
                    throw new IllegalArgumentException("Unauthorized WebSocket connection: " + e.getMessage(), e);
                }
            } else {
                log.warn("✗ Missing or invalid Authorization header in WebSocket CONNECT frame");
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
        }
        return message;
    }
}
