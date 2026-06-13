package com.se361.iam_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to authenticate a user")
public class LoginRequest {
    @NotBlank
    @Email
    @Schema(description = "User's email address", example = "admin@bds.com")
    private String email;

    @NotBlank
    @Schema(description = "User's password", example = "password")
    private String password;

    @Schema(description = "Whether to remember the user", example = "true")
    private Boolean rememberMe;
}
