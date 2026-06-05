package com.se361.iam_service.controller;

import com.se361.iam_service.entity.User;
import com.se361.iam_service.repository.UserRepository;
import com.se361.iam_service.service.UserService;
import com.se361.iam_service.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/users/validate")
    public ResponseEntity<Map<String, Boolean>> validateUser(
            @RequestParam UUID userId,
            @RequestParam String role) {
        try {
            User user = userService.findById(userId);
            boolean active = user.getStatus() == Constants.StatusProfileEnum.ACTIVE 
                    && user.getRole().name().equalsIgnoreCase(role);
            return ResponseEntity.ok(Map.of("active", active));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("active", false));
        }
    }

    @GetMapping("/api/users/{userId}/fcm-token")
    public ResponseEntity<String> getUserFcmToken(@PathVariable UUID userId) {
        try {
            User user = userService.findById(userId);
            String token = user.getFcmToken();
            return ResponseEntity.ok(token != null ? token : "");
        } catch (Exception e) {
            return ResponseEntity.ok("");
        }
    }

    @PutMapping("/api/users/{userId}/fcm-token")
    public ResponseEntity<Void> updateUserFcmToken(
            @PathVariable UUID userId,
            @RequestParam String token) {
        try {
            User user = userService.findById(userId);
            user.setFcmToken(token);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam String status) {
        try {
            userService.updateStatus(userId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
