package com.se361.iam_service.dto.request;

import com.se361.iam_service.util.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a new user")
public class RegisterRequest {
    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "{not_blank}")
    @Email(message = "{invalid_email}")
    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's phone number", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Ward ID for user's address", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID wardId;

    @Size(max = 50, message = "{max_length}")
    @Schema(description = "ID or Passport number", example = "123456789")
    private String identificationNumber;

    @Schema(description = "User's date of birth", example = "1990-01-01")
    private LocalDate dayOfBirth;

    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's gender", example = "MALE")
    private String gender;

    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's nationality", example = "Vietnamese")
    private String nation;

    @Schema(description = "ID issuance date", example = "2015-05-20")
    private LocalDate issuedDate;

    @Size(max = 50, message = "{max_length}")
    @Schema(description = "Authority that issued the ID", example = "Cục Cảnh sát QLHC về trật tự xã hội")
    private String issuingAuthority;

    @Schema(description = "Front side of identification document")
    private MultipartFile frontIdPicture;

    @Schema(description = "Back side of identification document")
    private MultipartFile backIdPicture;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    @Schema(description = "User's password", example = "password123")
    private String password;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    @Schema(description = "Password confirmation", example = "password123")
    private String passwordConfirm;

    // Role: CUSTOMER hoặc PROPERTY_OWNER
    @Schema(description = "User's requested role", example = "CUSTOMER")
    private Constants.RoleEnum role;
}
