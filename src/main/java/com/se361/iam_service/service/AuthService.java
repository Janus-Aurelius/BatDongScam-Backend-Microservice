package com.se361.iam_service.service;

import com.se361.iam_service.dto.response.TokenResponse;

import java.util.UUID;

public interface AuthService {
    TokenResponse login(String email, String password);
    TokenResponse refresh(String bearerToken);
    TokenResponse generateTokens(UUID userId);
}
