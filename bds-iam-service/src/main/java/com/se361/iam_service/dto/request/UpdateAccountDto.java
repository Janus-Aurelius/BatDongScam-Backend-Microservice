package com.se361.iam_service.dto.request;

import jakarta.validation.constraints.Email;
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
public class UpdateAccountDto {
    @Size(max = 50, message = "{max_length}")
    private String firstName;

    @Size(max = 50, message = "{max_length}")
    private String lastName;

    @Email(message = "{invalid_email}")
    @Size(max = 50, message = "{max_length}")
    private String email;

    @Size(max = 50, message = "{max_length}")
    private String phoneNumber;

    @Size(max = 50, message = "{max_length}")
    private String zaloContract;

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

    private MultipartFile avatar;

    private MultipartFile frontIdPicture;

    private MultipartFile backIdPicture;
}
