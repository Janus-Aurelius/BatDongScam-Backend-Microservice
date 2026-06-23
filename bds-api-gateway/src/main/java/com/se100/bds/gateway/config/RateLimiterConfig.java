package com.se100.bds.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Configuration for rate limiting in the API Gateway.
 * Provides KeyResolvers to isolate rate limits by user ID or remote IP address.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * KeyResolver that identifies callers using:
     * 1. The X-User-Id header if present (for authenticated sessions).
     * 2. The remote IP address (for unauthenticated endpoints like login and registration).
     */
    @Bean
    public KeyResolver ipOrUserKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Check for authenticated user id passed via headers
            String userId = request.getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.trim().isEmpty()) {
                return Mono.just("user:" + userId);
            }
            
            // Fallback to client IP address for anonymous traffic
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just("ip:" + remoteAddress.getAddress().getHostAddress());
            }
            
            return Mono.just("ip:anonymous");
        };
    }
}
