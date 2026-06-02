package com.se361.iam_service.dto.request;

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
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String zaloContract;
    private UUID wardId;
    private String identificationNumber;
    private LocalDate dayOfBirth;
    private String gender;
    private String nation;
    private LocalDate issuedDate;
    private String issuingAuthority;
    private MultipartFile avatar;
    private MultipartFile frontIdPicture;
    private MultipartFile backIdPicture;
}
