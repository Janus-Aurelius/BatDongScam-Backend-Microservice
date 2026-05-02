package com.se100.bds.dtos.requests.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RegisterRequest {

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    @Schema(
            name = "firstName",
            description = "First name of the user",
            type = "String",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Minh"
    )
    private String firstName;

    @NotBlank(message = "{not_blank}")
    @Size(max = 50, message = "{max_length}")
    @Schema(
            name = "lastName",
            description = "Last name of the user",
            type = "String",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Phan Dinh"
    )
    private String lastName;

    @NotBlank(message = "{not_blank}")
    @Email(message = "{invalid_email}")
    @Size(max = 100, message = "{max_length}")
    @Schema(
            name = "email",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "mail@email.com"
    )
    private String email;

    @Size(max = 20, message = "{max_length}")
    @Schema(
            name = "phoneNumber",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "0901234567"
    )
    private String phoneNumber;

    @Schema(
            name = "wardId",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private UUID wardId;

    @Schema(
            name = "identificationNumber",
            description = "CCCD number",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "123452134"
    )
    private String identificationNumber;

    @Schema(
            name = "dayOfBirth",
            description = "Day of Birth",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2000-01-15"
    )
    private LocalDate dayOfBirth;

    @Schema(
            name = "gender",
            description = "Gender",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "MALE"
    )
    private String gender;

    @Schema(
            name = "nation",
            description = "Nation",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Vietnamese"
    )
    private String nation;

    @Schema(
            name = "issuedDate",
            description = "The date that you received your CCCD",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2020-01-15"
    )
    private LocalDate issuedDate;

    @Schema(
            name = "issuingAuthority",
            description = "Where did you receive your CCCD",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Public Security Department"
    )
    private String issuingAuthority;

    @Schema(
            name = "frontIdPicture",
            description = "Front photo of CCCD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MultipartFile frontIdPicture;

    @Schema(
            name = "backIdPicture",
            description = "Back photo of CCCD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MultipartFile backIdPicture;

    @NotBlank(message = "{not_blank}")
    @Schema(
            name = "password",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "P@sswd123."
    )
    private String password;

    @NotBlank(message = "{not_blank}")
    @Schema(
            name = "passwordConfirm",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "P@sswd123."
    )
    private String passwordConfirm;

}