package com.se361.iam_service.controller;

import com.se361.iam_service.controller.base.AbstractBaseController;
import com.se361.iam_service.dto.request.UpdateAccountDto;
import com.se361.iam_service.dto.response.PageResponse;
import com.se361.iam_service.dto.response.SingleResponse;
import com.se361.iam_service.dto.response.UserResponse;
import com.se361.iam_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController extends AbstractBaseController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<SingleResponse<UserResponse>> me() {
        return responseFactory.successSingle(userService.getMe(), "OK");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<SingleResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        return responseFactory.successSingle(userService.getUserById(userId), "OK");
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SingleResponse<UserResponse>> updateMe(
            @ModelAttribute UpdateAccountDto dto
    ) {
        return responseFactory.successSingle(userService.updateMe(dto), "Updated");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SingleResponse<UserResponse>> updateUserById(
            @PathVariable UUID userId,
            @ModelAttribute UpdateAccountDto dto
    ) {
        return responseFactory.successSingle(userService.updateUserById(userId, dto), "Updated");
    }

    @DeleteMapping("/me")
    public ResponseEntity<SingleResponse<Void>> deleteMe() {
        userService.deleteMyAccount();
        return responseFactory.successSingle(null, "Deleted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<SingleResponse<Void>> deleteById(@PathVariable UUID userId) {
        userService.deleteAccountById(userId);
        return responseFactory.successSingle(null, "Deleted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{propOwnerId}/{approve}/approve")
    public ResponseEntity<SingleResponse<Void>> approveAccount(
            @PathVariable UUID propOwnerId,
            @PathVariable Boolean approve
    ) {
        userService.approveAccount(propOwnerId, approve);
        return responseFactory.successSingle(null, approve ? "Approved" : "Rejected");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getUsersByRole(
            @RequestParam String role,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<UserResponse> result = userService.getAllByRole(role, name, pageable);
        return responseFactory.successPage(result, "OK");
    }
}
