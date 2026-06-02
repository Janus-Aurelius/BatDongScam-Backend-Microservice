package com.se361.iam_service.controller;

import com.se361.iam_service.dto.request.LoginRequest;
import com.se361.iam_service.dto.request.RegisterRequest;
import com.se361.iam_service.dto.response.TokenResponse;
import com.se361.iam_service.service.AuthService;
import com.se361.iam_service.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    //private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    @PostMapping("/register")
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
        //userService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Authorization") String bearerToken
    ) {
        return ResponseEntity.ok(authService.refresh(bearerToken));
    }
}
