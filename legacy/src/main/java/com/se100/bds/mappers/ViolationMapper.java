package com.se100.bds.mappers;

import com.se100.bds.dtos.responses.violation.*;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.entities.violation.ViolationReport;
import com.se100.bds.services.domains.ranking.RankingService;
import com.se100.bds.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ViolationMapper extends BaseMapper {

    private final RankingService rankingService;

    @Autowired
    public ViolationMapper(ModelMapper modelMapper, RankingService rankingService) {
        super(modelMapper);
        this.rankingService = rankingService;
    }

    @Override
    protected void configureCustomMappings() {
        // No custom mappings needed for now
    }

    public ViolationAdminItem toAdminItem(ViolationReport violation, User reportedUser, Property reportedProperty) {
        ViolationAdminItem item = ViolationAdminItem.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .violationType(violation.getViolationType())
                .status(violation.getStatus())
                .description(violation.getDescription())
                .reportedAt(violation.getCreatedAt())
                .build();

        // Reporter info (always present - NOT NULL)
        User reporter = violation.getReporterUser();
        item.setReporterName(reporter.getFirstName() + " " + reporter.getLastName());
        item.setReporterAvatarUrl(reporter.getAvatarUrl());

        // Reported info (user or property)
        if (reportedUser != null) {
            item.setReportedName(reportedUser.getFirstName() + " " + reportedUser.getLastName());
            item.setReportedAvatarUrl(reportedUser.getAvatarUrl());
        } else if (reportedProperty != null) {
            item.setReportedName(reportedProperty.getTitle());

            // Safely access mediaList (might be lazy-loaded)
            try {
                if (reportedProperty.getMediaList() != null && !reportedProperty.getMediaList().isEmpty()) {
                    item.setReportedAvatarUrl(reportedProperty.getMediaList().get(0).getFilePath());
                }
            } catch (Exception e) {
                log.warn("Could not access property mediaList for violation mapping: {}", e.getMessage());
                item.setReportedAvatarUrl(null);
            }
        }

        return item;
    }

    public ViolationUserItem toUserItem(ViolationReport violation, String targetName) {
        return ViolationUserItem.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .violationType(violation.getViolationType())
                .description(violation.getDescription())
                .status(violation.getStatus())
                .targetName(targetName)
                .resolvedAt(violation.getResolvedAt())
                .reportedAt(violation.getCreatedAt())
                .build();
    }

    public ViolationUserDetails toUserDetails(ViolationReport violation, String targetName) {
        List<String> evidenceUrls = violation.getMediaList() != null
                ? violation.getMediaList().stream()
                    .map(Media::getFilePath)
                    .collect(Collectors.toList())
                : List.of();

        return ViolationUserDetails.builder()
                .id(violation.getId())
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .violationType(violation.getViolationType())
                .status(violation.getStatus())
                .reportedAt(violation.getCreatedAt())
                .targetName(targetName)
                .description(violation.getDescription())
                .resolvedAt(violation.getResolvedAt())
                .evidenceUrls(evidenceUrls)
                .penaltyApplied(violation.getPenaltyApplied())
                .resolutionNotes(violation.getResolutionNotes())
                .build();
    }

    public ViolationAdminDetails toAdminDetails(ViolationReport violation, User reportedUser, Property reportedProperty) {
        ViolationAdminDetails details = ViolationAdminDetails.builder()
                .id(violation.getId())
                .violationType(violation.getViolationType())
                .reportedAt(violation.getCreatedAt())
                .description(violation.getDescription())
                .violationStatus(violation.getStatus())
                .resolutionNotes(violation.getResolutionNotes())
                .build();

        // Reporter info (always present - NOT NULL)
        details.setReporter(buildViolationUser(violation.getReporterUser()));

        // Reported user info
        if (reportedUser != null) {
            details.setReportedUser(buildViolationUser(reportedUser));
        }

        // Reported property info
        if (reportedProperty != null) {
            details.setReportedProperty(buildViolationProperty(reportedProperty));
        }

        // Media URLs
        if (violation.getMediaList() != null) {
            List<String> imageUrls = violation.getMediaList().stream()
                    .filter(m -> m.getMediaType() == Constants.MediaTypeEnum.IMAGE)
                    .map(Media::getFilePath)
                    .collect(Collectors.toList());

            List<String> documentUrls = violation.getMediaList().stream()
                    .filter(m -> m.getMediaType() == Constants.MediaTypeEnum.DOCUMENT)
                    .map(Media::getFilePath)
                    .collect(Collectors.toList());

            details.setImageUrls(imageUrls);
            details.setDocumentUrls(documentUrls);
        }

        return details;
    }

    private ViolationAdminDetails.ViolationUser buildViolationUser(User user) {
        if (user == null) return null;

        String role = user.getRole() != null ? user.getRole().name() : null;

        String userTier;
        try {
            userTier = rankingService.getCurrentTier(user.getId(), user.getRole());
        } catch (Exception e) {
            log.error("Failed to get user's tier for violation: {}", e.getMessage());
            userTier = null;
        }

        return ViolationAdminDetails.ViolationUser.builder()
                .id(user.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(role)
                .userTier(userTier)
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    private ViolationAdminDetails.ViolationProperty buildViolationProperty(Property property) {
        if (property == null) return null;

        String location = null;
        if (property.getWard() != null && property.getWard().getDistrict() != null) {
            String district = property.getWard().getDistrict().getDistrictName();
            String city = property.getWard().getDistrict().getCity() != null
                    ? property.getWard().getDistrict().getCity().getCityName() : null;

            if (district != null && city != null) {
                location = district + ", " + city;
            } else if (city != null) {
                location = city;
            } else {
                location = district;
            }
        }

        String thumbnailUrl = null;
        try {
            if (property.getMediaList() != null && !property.getMediaList().isEmpty()) {
                thumbnailUrl = property.getMediaList().get(0).getFilePath();
            }
        } catch (Exception e) {
            log.warn("Could not access property mediaList in buildViolationProperty: {}", e.getMessage());
        }

        String propertyTypeName = property.getPropertyType() != null
                ? property.getPropertyType().getTypeName() : null;

        return ViolationAdminDetails.ViolationProperty.builder()
                .id(property.getId())
                .title(property.getTitle())
                .propertyTypeName(propertyTypeName)
                .thumbnailUrl(thumbnailUrl)
                .transactionType(property.getTransactionType())
                .location(location)
                .price(property.getPriceAmount())
                .totalArea(property.getArea())
                .build();
    }
}

