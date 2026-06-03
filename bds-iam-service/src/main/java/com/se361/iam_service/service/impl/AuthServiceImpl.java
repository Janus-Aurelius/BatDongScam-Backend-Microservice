package com.se361.iam_service.service.impl;

import com.se361.iam_service.dto.response.TokenResponse;
import com.se361.iam_service.entity.User;
import com.se361.iam_service.repository.UserRepository;
import com.se361.iam_service.security.JwtTokenProvider;
import com.se361.iam_service.security.JwtUserDetails;
import com.se361.iam_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TokenResponse login(String email, String password, Boolean rememberMe) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        JwtUserDetails principal = (JwtUserDetails) authentication.getPrincipal();
        UUID userId = UUID.fromString(principal.getId());

        userRepository.findById(userId)
                .ifPresent(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    userRepository.save(user);
                });
        return generateTokens(userId, rememberMe);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String bearerToken) {
        String token = jwtTokenProvider.extractFromBearer(bearerToken);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UUID userId = UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
        return generateTokens(userId, false);
    }

    @Override
    public TokenResponse generateTokens(UUID userId, Boolean rememberMe) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return TokenResponse.builder()
                .token(jwtTokenProvider.generateJwt(userId.toString()))
                .refreshToken(jwtTokenProvider.generateRefresh(userId.toString(), rememberMe))
                .userId(userId.toString())
                .role(user.getRole().name())
                .build();
    }
}
