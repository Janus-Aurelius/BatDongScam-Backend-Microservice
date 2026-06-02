package com.se361.iam_service.dto.response;

import com.se361.iam_service.util.Constants;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String avatarUrl;
    private Constants.RoleEnum role;
    private Constants.StatusProfileEnum status;
    private String gender;
    private String nation;
    private LocalDate dayOfBirth;
    private String identificationNumber;
    private LocalDate issueDate;
    private String issuingAuthority;
    private String zaloContact;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankBin;
    private UUID wardId;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private SaleAgentProfile saleAgentProfile;
    private PropertyOwnerProfile propertyOwnerProfile;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleAgentProfile {
        private String employeeCode;
        private int maxProperties;
        private LocalDateTime hiredDate;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyOwnerProfile {
        private LocalDateTime approvedAt;
    }
}
