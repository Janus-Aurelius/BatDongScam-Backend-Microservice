package com.se100.bds.services.domains.auth;

import com.se100.bds.dtos.responses.auth.TokenResponse;

import java.util.UUID;

public interface AuthService {
    TokenResponse login(String email, String password, Boolean rememberMe);

    TokenResponse refreshFromBearerString(String bearer);

    TokenResponse refresh(String refreshToken);

    TokenResponse generateTokens(UUID id, Boolean rememberMe);
}