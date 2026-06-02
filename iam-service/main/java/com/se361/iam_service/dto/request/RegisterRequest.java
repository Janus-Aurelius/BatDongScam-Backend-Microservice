package com.se361.iam_service.dto.request;

import com.se361.iam_service.util.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phoneNumber;

    private UUID wardId;

    private String identificationNumber;

    private LocalDate dayOfBirth;

    private String gender;

    private String nation;

    private LocalDate issuedDate;

    private String issuingAuthority;

    private MultipartFile frontIdPicture;

    private MultipartFile backIdPicture;

    @NotBlank
    private String password;

    @NotBlank
    private String passwordConfirm;

    // Role: CUSTOMER hoặc PROPERTY_OWNER
    private Constants.RoleEnum role;
}
