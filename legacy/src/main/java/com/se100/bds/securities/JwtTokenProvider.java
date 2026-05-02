package com.se100.bds.securities;

import com.se100.bds.models.entities.user.User;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.services.domains.user.UserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static com.se100.bds.utils.Constants.TOKEN_HEADER;
import static com.se100.bds.utils.Constants.TOKEN_TYPE;

@Component
@Slf4j
public class JwtTokenProvider {
    private final UserService userService;

    private final String appSecret;

    @Getter
    private final Long tokenExpiresIn;

    @Getter
    private Long refreshTokenExpiresIn;

    public JwtTokenProvider(
            @Value("${app.secret}") final String appSecret,
            @Value("${app.jwt.token.expires-in}") final Long tokenExpiresIn,
            @Value("${app.jwt.refresh-token.expires-in}") final Long refreshTokenExpiresIn,
            final UserService userService
    ) {
        this.userService = userService;
        this.appSecret = appSecret;
        this.tokenExpiresIn = tokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    public String generateTokenByUserId(final String id, final Long expires) {
        String token = Jwts.builder()
                .setSubject(id)
                .setIssuedAt(new Date())
                .setExpiration(getExpireDate(expires))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        log.trace("Token is added to the local cache for userID: {}, ttl: {}", id, expires);

        return token;
    }

    public String generateJwt(final String id) {
        return generateTokenByUserId(id, tokenExpiresIn);
    }

    public String generateRefresh(final String id) {
        return generateTokenByUserId(id, refreshTokenExpiresIn);
    }

    public JwtUserDetails getPrincipal(final Authentication authentication) {
        return userService.getPrincipal(authentication);
    }

    public String getUserIdFromToken(final String token) {
        Claims claims = parseToken(token).getBody();

        return claims.getSubject();
    }

    /**
     * Get user from token.
     *
     * @param token String
     * @return User
     */
    public User getUserFromToken(final String token) {
        try {
            return userService.findById(UUID.fromString(getUserIdFromToken(token)));
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Boolean result of whether token is valid or not.
     * Stateless validation - only checks signature and expiration.
     *
     * @param token String token
     * @return boolean
     */
    public boolean validateToken(final String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("[JWT] Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateToken(final String token, final HttpServletRequest httpServletRequest) {
        try {
            parseToken(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("[JWT] Invalid JWT signature!");
            httpServletRequest.setAttribute("invalid", "Invalid JWT signature!");
        } catch (UnsupportedJwtException e) {
            log.error("[JWT] Unsupported JWT token!");
            httpServletRequest.setAttribute("unsupported", "Unsupported JWT token!");
        } catch (MalformedJwtException e) {
            log.error("[JWT] Invalid JWT token!");
            httpServletRequest.setAttribute("invalid", "Invalid JWT token!");
        } catch (ExpiredJwtException e) {
            log.error("[JWT] Expired JWT token!");
            httpServletRequest.setAttribute("expired", "Expired JWT token!");
        } catch (IllegalArgumentException e) {
            log.error("[JWT] Jwt claims string is empty");
            httpServletRequest.setAttribute("illegal", "JWT claims string is empty.");
        }

        return false;
    }

    public String extractJwtFromBearerString(final String bearer) {
        if (StringUtils.hasText(bearer) && bearer.startsWith(String.format("%s ", TOKEN_TYPE))) {
            return bearer.substring(TOKEN_TYPE.length() + 1);
        }

        return null;
    }

    public String extractJwtFromRequest(final HttpServletRequest request) {
        return extractJwtFromBearerString(request.getHeader(TOKEN_HEADER));
    }

    private Jws<Claims> parseToken(final String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
    }

    private boolean isTokenExpired(final String token) {
        return parseToken(token).getBody().getExpiration().before(new Date());
    }

    private Date getExpireDate(final Long expires) {
        return new Date(new Date().getTime() + expires);
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(appSecret.getBytes());
    }
}
