package com.se361.iam_service.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

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

    private final Environment env;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public JwtTokenProvider(
            @Value("${app.secret}") String appSecret,
            @Value("${app.jwt.token.expires-in}") Long tokenExpiresIn,
            @Value("${app.jwt.refresh-token.expires-in}") Long refreshTokenExpiresIn,
            @Value("${app.private-key:}") String privateKeyPem,
            Environment env
    ) {
        this.appSecret             = appSecret;
        this.tokenExpiresIn        = tokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
        this.env                   = env;
        initializeKeys(privateKeyPem);
    }

    @PostConstruct
    public void checkEnv() {
        if (appSecret == null || "BatDongScamSecretKeyForJWTTokenAtLeast256BitsLong!!".equals(appSecret)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: APP_SECRET is missing or using the insecure default fallback. " +
                    "Please copy '.env.example' to '.env' and set custom values before starting the application.");
        }
    }

    private void initializeKeys(String privateKeyPem) {
        if (StringUtils.hasText(privateKeyPem)) {
            try {
                String cleanedPem = privateKeyPem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "")
                        .replace("-----END PRIVATE KEY-----", "");
                byte[] encoded = Base64.getDecoder().decode(cleanedPem);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
                this.privateKey = keyFactory.generatePrivate(keySpec);

                if (this.privateKey instanceof RSAPrivateCrtKey) {
                    RSAPrivateCrtKey rsaPrivateCrtKey = (RSAPrivateCrtKey) this.privateKey;
                    RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                            rsaPrivateCrtKey.getModulus(),
                            rsaPrivateCrtKey.getPublicExponent()
                    );
                    this.publicKey = keyFactory.generatePublic(publicKeySpec);
                    log.info("Successfully loaded RSA private key and derived public key.");
                } else {
                    throw new IllegalArgumentException("Private key must be an RSAPrivateCrtKey (PKCS#8 RSA private key).");
                }
            } catch (Exception e) {
                log.error("Failed to parse RSA private key from configuration: {}", e.getMessage());
                throw new IllegalStateException("Failed to parse RSA private key", e);
            }
        } else {
            // Check if we are in production
            boolean isProduction = true;
            if (env != null) {
                List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
                if (activeProfiles.isEmpty() || activeProfiles.contains("local") || activeProfiles.contains("test") || activeProfiles.contains("dev")) {
                    isProduction = false;
                }
            }
            if (isProduction) {
                throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: RSA private key (app.private-key) must be configured in production!");
            }

            // Fallback: Generate a key pair dynamically for local/dev
            try {
                log.warn("app.private-key is empty. Generating a temporary in-memory RSA key pair. Tokens will be invalidated upon application restart!");
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                this.privateKey = kp.getPrivate();
                this.publicKey = kp.getPublic();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("RSA algorithm not supported", e);
            }
        }
    }

    public String generateJwt(String userId) {
        return generateJwt(userId, null);
    }

    public String generateJwt(String userId, String roles) {
        return buildToken(userId, tokenExpiresIn, roles);
    }

    public String generateRefresh(String userId) {
        return generateRefresh(userId, false);
    }

    public String generateRefresh(String userId, Boolean rememberMe) {
        long expiresIn = refreshTokenExpiresIn;
        if (rememberMe != null && rememberMe) {
            expiresIn *= 30;
        }
        return buildToken(userId, expiresIn, null);
    }

    public String getUserIdFromToken(String token) {
        return parseToken(token).getPayload().getSubject();
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

    // Expose public key modulus & exponent for JWKS
    public Map<String, Object> getJwks() {
        Map<String, Object> jwk = new HashMap<>();
        if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPub = (RSAPublicKey) publicKey;
            String modulus = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPub.getModulus().toByteArray());
            String exponent = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPub.getPublicExponent().toByteArray());
            jwk.put("kty", "RSA");
            jwk.put("use", "sig");
            jwk.put("alg", "RS256");
            jwk.put("kid", "bds-key-id");
            jwk.put("n", modulus);
            jwk.put("e", exponent);
        }
        return Map.of("keys", List.of(jwk));
    }

    private String buildToken(String subject, Long expiresIn, String roles) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresIn))
                .signWith(privateKey);
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.compact();
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token);
    }
}
