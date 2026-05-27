package com.se100.bds.dtos.requests.account;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for updating user account information.
 * <p>
 * All fields are optional - only include fields you want to update.
 * <p>
 * For file upload fields (avatar, frontIdPicture, backIdPicture):
 * - To update: send the file as multipart/form-data
 * - To skip update: OMIT the field entirely from the request (do NOT send empty string)
 * - Empty string values will cause validation errors
 */
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
