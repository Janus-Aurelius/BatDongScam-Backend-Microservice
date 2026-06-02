package com.se361.iam_service.service;

import com.se361.iam_service.dto.request.RegisterRequest;
import com.se361.iam_service.dto.request.UpdateAccountDto;
import com.se361.iam_service.dto.response.UserResponse;
import com.se361.iam_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {
    User getCurrentUser();
    User findById(UUID id);
    User findByEmail(String email);

    // Me / profile
    UserResponse getMe();
    UserResponse getUserById(UUID id);
    UserResponse updateMe(UpdateAccountDto dto);
    UserResponse updateUserById(UUID id, UpdateAccountDto dto);

    // Delete
    void deleteMyAccount();
    void deleteAccountById(UUID id);

    // Register
    User register(RegisterRequest request);

    // Admin ops
    void approveAccount(UUID propOwnerId, boolean approve);
    User updateStatus(UUID id, String status);

    // List (phần ranking/tier sẽ là stub trả null — bổ sung sau khi có Ranking Service)
    Page<UserResponse> getAllByRole(String role, String name, Pageable pageable);

    // Internal helpers (dùng bởi các service khác trong IAM)
    List<UUID> getAllCurrentAgentIds();
    void activateUser(UUID id);
}
