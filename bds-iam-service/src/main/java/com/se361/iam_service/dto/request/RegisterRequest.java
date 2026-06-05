package com.se361.iam_service.dto.request;

import com.se361.iam_service.util.Constants;
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
public class RegisterRequest {
    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    private String firstName;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    private String lastName;

    @NotBlank(message = "{not_blank}")
    @Email(message = "{invalid_email}")
    @Size(max = 50, message = "{max_length}")
    private String email;

    @Size(max = 50, message = "{max_length}")
    private String phoneNumber;

    private UUID wardId;

    @Size(max = 50, message = "{max_length}")
    private String identificationNumber;

    private LocalDate dayOfBirth;

    @Size(max = 50, message = "{max_length}")
    private String gender;

    @Size(max = 50, message = "{max_length}")
    private String nation;

    private LocalDate issuedDate;

    @Size(max = 50, message = "{max_length}")
    private String issuingAuthority;

    private MultipartFile frontIdPicture;

    private MultipartFile backIdPicture;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    private String password;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    private String passwordConfirm;

    // Role: CUSTOMER hoặc PROPERTY_OWNER
    private Constants.RoleEnum role;
}
