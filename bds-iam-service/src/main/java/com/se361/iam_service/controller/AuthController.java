package com.se361.iam_service.controller;

import com.se361.iam_service.dto.request.LoginRequest;
import com.se361.iam_service.dto.request.RegisterRequest;
import com.se361.iam_service.dto.response.TokenResponse;
import com.se361.iam_service.service.AuthService;
import com.se361.iam_service.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.se361.iam_service.security.JwtTokenProvider;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user login, registration, and token management")
public class AuthController {

    private final AuthService authService;
    private final com.se361.iam_service.service.UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Login to the system", description = "Authenticates user with email and password, returning access and refresh tokens.")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword(), request.getRememberMe()));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Allows CUSTOMER or PROPERTY_OWNER to register. Admin and Sales Agent registration must be handled by an administrator.")
    public ResponseEntity<Void> register(
            @Valid @ModelAttribute RegisterRequest request
    ) {
        // Chỉ cho phép CUSTOMER và PROPERTY_OWNER tự đăng ký
        Constants.RoleEnum role = request.getRole();
        if (role == null ||
                role == Constants.RoleEnum.ADMIN ||
                role == Constants.RoleEnum.SALESAGENT) {
            return ResponseEntity.badRequest().build();
        }
        userService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Authorization") String bearerToken
    ) {
        return ResponseEntity.ok(authService.refresh(bearerToken));
    }

    @GetMapping("/jwks")
    public ResponseEntity<Map<String, Object>> getJwks() {
        return ResponseEntity.ok(jwtTokenProvider.getJwks());
    }
}
