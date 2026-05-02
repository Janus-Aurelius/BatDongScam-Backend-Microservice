package com.se100.bds.services.domains.auth.impl;

import com.se100.bds.dtos.responses.auth.TokenResponse;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.exceptions.RefreshTokenExpiredException;
import com.se100.bds.securities.JwtTokenProvider;
import com.se100.bds.securities.JwtUserDetails;
import com.se100.bds.services.MessageSourceService;
import com.se100.bds.services.domains.auth.AuthService;
import com.se100.bds.services.domains.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final MessageSourceService messageSourceService;

    /**
     * Authenticate user.
     *
     * @param email      String
     * @param password   String
     * @param rememberMe Boolean
     * @return TokenResponse
     */
    @Override
    @Transactional
    public TokenResponse login(String email, final String password, final Boolean rememberMe) {
        log.info("Login request received: {}", email);

        String badCredentialsMessage = messageSourceService.get("Unauthorized");

        User user;
        try {
            user = userService.findByEmail(email);
            email = user.getEmail();
        } catch (NotFoundException e) {
            log.error("User not found with email: {}", email);
            throw new AuthenticationCredentialsNotFoundException(badCredentialsMessage);
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(email, password);
        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            JwtUserDetails jwtUserDetails = jwtTokenProvider.getPrincipal(authentication);

            // Active the user
            userService.activeteUser(user.getId().toString());

            return generateTokens(UUID.fromString(jwtUserDetails.getId()), rememberMe);
        } catch (NotFoundException e) {
            log.error("Authentication failed for email: {}", email);
            throw new AuthenticationCredentialsNotFoundException(badCredentialsMessage);
        }
    }

    /**
     * Refresh from bearer string.
     *
     * @param bearer String
     * @return TokenResponse
     */
    @Override
    @Transactional
    public TokenResponse refreshFromBearerString(final String bearer) {
        return refresh(jwtTokenProvider.extractJwtFromBearerString(bearer));
    }

    @Override
    @Transactional
    public TokenResponse refresh(final String refreshToken) {
        log.info("Refresh request received");

        // Validate refresh token
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.error("Refresh token is null or empty");
            throw new RefreshTokenExpiredException("Refresh token is required");
        }

        // Check if refresh token is valid and not expired
        try {
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.error("Refresh token is invalid");
                throw new RefreshTokenExpiredException("Invalid refresh token");
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Refresh token has expired");
            throw new RefreshTokenExpiredException("Refresh token expired. Please login again.");
        } catch (Exception e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            throw new RefreshTokenExpiredException("Invalid refresh token");
        }

        // Extract user from refresh token
        User user = jwtTokenProvider.getUserFromToken(refreshToken);
        if (user == null) {
            log.error("User not found from refresh token");
            throw new RefreshTokenExpiredException("Invalid refresh token - user not found");
        }

        log.info("Generating new tokens for user: {}", user.getId());
        // Generate new access token and new refresh token
        return generateTokens(user.getId(), false);
    }

    /**
     * Generate access token and refresh token for the user.
     * Stateless implementation - tokens are NOT saved to Redis.
     */
    @Override
    @Transactional
    public TokenResponse generateTokens(final UUID id, final Boolean rememberMe) {
        String token = jwtTokenProvider.generateJwt(id.toString());
        String refreshToken = jwtTokenProvider.generateRefresh(id.toString());

        log.info("Token generated for user: {}", id);


        User user = userService.findById(id);
        String role = user.getRole().getValue();

        return TokenResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(String.valueOf(id))
                .role(role)
                .build();
    }
}