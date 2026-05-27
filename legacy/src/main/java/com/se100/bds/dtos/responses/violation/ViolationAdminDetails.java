package com.se100.bds.dtos.responses.violation;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import com.se100.bds.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ViolationAdminDetails extends AbstractBaseDataResponse {
    private Constants.ViolationTypeEnum violationType;
    private LocalDateTime reportedAt;
    private String description;
    private ViolationUser reporter;
    private ViolationUser reportedUser;
    private ViolationProperty reportedProperty;
    private List<String> imageUrls;
    private List<String> documentUrls;
    private Constants.ViolationStatusEnum violationStatus;
    private String resolutionNotes;

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder
    public static class ViolationUser {
        private UUID id;
        private String fullName;
        private String role;
        private String userTier;
        private String email;
        private String phoneNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @SuperBuilder
    public static class ViolationProperty {
        private UUID id;
        private String title;
        private String propertyTypeName;
        private String thumbnailUrl;
        private Constants.TransactionTypeEnum transactionType;
        private String location;
        private BigDecimal price;
        private BigDecimal totalArea;
    }
}
