package com.se361.iam_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_TYPE   = "Bearer";

    private final String appSecret;

    @Getter
    private final Long tokenExpiresIn;

    @Getter
    private final Long refreshTokenExpiresIn;

    public JwtTokenProvider(
            @Value("${app.secret}") String appSecret,
            @Value("${app.jwt.token.expires-in}") Long tokenExpiresIn,
            @Value("${app.jwt.refresh-token.expires-in}") Long refreshTokenExpiresIn
    ) {
        this.appSecret             = appSecret;
        this.tokenExpiresIn        = tokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    public String generateJwt(String userId) {
        return buildToken(userId, tokenExpiresIn);
    }

    public String generateRefresh(String userId) {
        return buildToken(userId, refreshTokenExpiresIn);
    }

    public String getUserIdFromToken(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("[JWT] Validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateToken(String token, HttpServletRequest request) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            request.setAttribute("expired", "Expired JWT token");
        } catch (MalformedJwtException e) {
            request.setAttribute("invalid", "Malformed JWT token");
        } catch (UnsupportedJwtException e) {
            request.setAttribute("unsupported", "Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            request.setAttribute("illegal", "JWT claims string is empty");
        } catch (Exception e) {
            request.setAttribute("invalid", "Invalid JWT token");
        }
        return false;
    }

    public String extractJwtFromRequest(HttpServletRequest request) {
        return extractFromBearer(request.getHeader(TOKEN_HEADER));
    }

    public String extractFromBearer(String bearer) {
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_TYPE + " ")) {
            return bearer.substring(TOKEN_TYPE.length() + 1);
        }
        return null;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String buildToken(String subject, Long expiresIn) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiresIn))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token);
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(appSecret.getBytes());
    }
}
