package com.se100.bds.gateway.filter;

import com.se.bds.common.dto.ApiResponse;
import com.se100.bds.gateway.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final AppProperties appProperties;
    private final Environment env;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip JWT validation for public paths
        if (isPublicPath(path)) {
            log.debug("Public path, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Get Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            String userId;
            String roles;

            if (isMockToken(token) && isTestOrLocalProfile()) {
                if ("admin".equals(token)) {
                    userId = "admin-id";
                    roles = "ADMIN";
                } else if ("agent1".equals(token)) {
                    userId = "agent-id";
                    roles = "SALESAGENT";
                } else if ("customer1".equals(token)) {
                    userId = "customer-id";
                    roles = "CUSTOMER";
                } else {
                    userId = "anonymous";
                    roles = "";
                }
                log.debug("Mock token bypass applied for: {}, userId: {}, roles: {}", token, userId, roles);
            } else {
                Claims claims = parseToken(token);
                userId = claims.getSubject();
                roles = claims.get("roles", String.class);
                if (roles == null) {
                    Object rolesObj = claims.get("roles");
                    if (rolesObj != null) {
                        roles = rolesObj.toString();
                    } else {
                        roles = "";
                    }
                }
                log.debug("JWT validated for user: {}, roles: {}, path: {}", userId, roles, path);
            }

            // Forward userId to downstream services via X-User-Id header and roles via X-User-Roles
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
            return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return onError(exchange, "Invalid JWT signature", HttpStatus.UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            return onError(exchange, "Malformed JWT token", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            return onError(exchange, "Unsupported JWT token", HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return onError(exchange, "JWT claims string is empty", HttpStatus.UNAUTHORIZED);
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -100; // High priority
    }

    private boolean isPublicPath(String path) {
        return appProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isMockToken(String token) {
        return "admin".equals(token) || "agent1".equals(token) || "customer1".equals(token);
    }

    private boolean isTestOrLocalProfile() {
        if (env == null) return true;
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        return activeProfiles.isEmpty() || activeProfiles.contains("local") || activeProfiles.contains("test") || activeProfiles.contains("dev");
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private javax.crypto.SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.getSecret().getBytes());
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        ApiResponse<Void> apiResponse = ApiResponse.error(message);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize API response", e);
            String fallback = "{\"success\":false,\"message\":\"" + message + "\",\"data\":null}";
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback.getBytes())));
        }
    }
}
