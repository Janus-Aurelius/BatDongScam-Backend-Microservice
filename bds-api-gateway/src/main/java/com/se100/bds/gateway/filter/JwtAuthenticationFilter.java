package com.se100.bds.gateway.filter;

import com.se.bds.common.dto.ApiResponse;
import com.se100.bds.gateway.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final AppProperties appProperties;
    private final Environment env;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.create();

    private volatile PublicKey cachedPublicKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (appProperties == null || appProperties.getPublicPaths() == null) {
            log.error("Gateway JWT Configuration missing: appProperties or publicPaths is null.");
            return onError(exchange, "Internal server configuration error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

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

        // Handle Mock Tokens for Test/Local profiles
        if (isMockToken(token) && isTestOrLocalProfile()) {
            String userId;
            String roles;
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

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        // Real Token validation (Reactive)
        return parseClaims(token)
                .onErrorResume(e -> {
                    log.error("JWT validation failed: {}", e.getMessage());
                    HttpStatus status = HttpStatus.UNAUTHORIZED;
                    String msg = "Invalid token";
                    if (e instanceof ExpiredJwtException) {
                        msg = "Token expired";
                    } else if (e instanceof SignatureException) {
                        msg = "Invalid JWT signature";
                    } else if (e instanceof MalformedJwtException) {
                        msg = "Malformed JWT token";
                    } else if (e instanceof UnsupportedJwtException) {
                        msg = "Unsupported JWT token";
                    } else if (e instanceof IllegalArgumentException) {
                        msg = "JWT claims string is empty";
                    }
                    return Mono.error(new JwtValidationException(msg, status));
                })
                .flatMap(claims -> {
                    String userId = claims.getSubject();
                    String roles = claims.get("roles", String.class);
                    if (roles == null) {
                        Object rolesObj = claims.get("roles");
                        roles = rolesObj != null ? rolesObj.toString() : "";
                    }
                    log.debug("JWT validated for user: {}, roles: {}, path: {}", userId, roles, path);

                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Roles", roles)
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(JwtValidationException.class, e -> onError(exchange, e.getMessage(), e.getStatus()));
    }

    private static class JwtValidationException extends RuntimeException {
        private final HttpStatus status;
        public JwtValidationException(String message, HttpStatus status) {
            super(message);
            this.status = status;
        }
        public HttpStatus getStatus() { return status; }
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

    private Mono<Claims> parseClaims(String token) {
        return getPublicKey()
                .flatMap(key -> {
                    try {
                        Claims claims = Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                        return Mono.just(claims);
                    } catch (SignatureException e) {
                        log.warn("JWT signature verification failed with cached key. Invalidating cache and retrying...");
                        this.cachedPublicKey = null; // Invalidate cache
                        return getPublicKey()
                                .map(newKey -> Jwts.parser()
                                        .verifyWith(newKey)
                                        .build()
                                        .parseSignedClaims(token)
                                        .getPayload());
                    }
                });
    }

    private Mono<PublicKey> getPublicKey() {
        if (cachedPublicKey != null) {
            return Mono.just(cachedPublicKey);
        }
        return fetchPublicKeyFromIam()
                .doOnNext(key -> this.cachedPublicKey = key);
    }

    private Mono<PublicKey> fetchPublicKeyFromIam() {
        String jwksUri = appProperties.getJwksUri();
        if (!StringUtils.hasText(jwksUri)) {
            jwksUri = "http://iam-service:8084/api/auth/jwks";
        }
        log.info("Fetching JWKS from IAM service: {}", jwksUri);
        return webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractPublicKeyFromJwks)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorMap(e -> new RuntimeException("Failed to fetch JWKS: " + e.getMessage(), e));
    }

    @SuppressWarnings("unchecked")
    private PublicKey extractPublicKeyFromJwks(Map<String, Object> jwks) {
        try {
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            if (keys == null || keys.isEmpty()) {
                throw new IllegalArgumentException("No keys found in JWKS response.");
            }
            Map<String, Object> jwk = keys.get(0);
            String nStr = (String) jwk.get("n");
            String eStr = (String) jwk.get("e");

            byte[] modulusBytes = Base64.getUrlDecoder().decode(nStr);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(eStr);

            java.math.BigInteger modulus = new java.math.BigInteger(1, modulusBytes);
            java.math.BigInteger exponent = new java.math.BigInteger(1, exponentBytes);

            java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(modulus, exponent);
            java.security.KeyFactory factory = java.security.KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract public key from JWKS", e);
        }
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
