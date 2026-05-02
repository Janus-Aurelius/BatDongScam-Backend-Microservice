package com.se100.bds.services.domains.violation.impl;

import com.se100.bds.dtos.requests.violation.UpdateViolationRequest;
import com.se100.bds.dtos.requests.violation.ViolationCreateRequest;
import com.se100.bds.dtos.responses.violation.*;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.mappers.ViolationMapper;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.entities.violation.ViolationReport;
import com.se100.bds.repositories.domains.violation.ViolationRepository;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.domains.violation.ViolationService;
import com.se100.bds.services.fileupload.CloudinaryService;
import com.se100.bds.utils.Constants;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {
    private final ViolationRepository violationRepository;
    private final UserService userService;
    private final PropertyService propertyService;
    private final ViolationMapper violationMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public Page<ViolationAdminItem> getAdminViolationItems(
            Pageable pageable,
            List<Constants.ViolationTypeEnum> violationTypes,
            List<Constants.ViolationStatusEnum> violationStatusEnums,
            String name,
            Integer month,
            Integer year
    ) {
        Specification<ViolationReport> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by violation types
            if (violationTypes != null && !violationTypes.isEmpty()) {
                predicates.add(root.get("violationType").in(violationTypes));
            }

            // Filter by status
            if (violationStatusEnums != null && !violationStatusEnums.isEmpty()) {
                predicates.add(root.get("status").in(violationStatusEnums));
            }

            // Filter by month and year
            if (month != null && year != null) {
                LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
                LocalDateTime endDate = startDate.plusMonths(1);
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startDate, endDate));
            } else if (year != null) {
                LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
                LocalDateTime endDate = startDate.plusYears(1);
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startDate, endDate));
            }

            // Filter by reporter name (JOIN with User entity)
            if (name != null && !name.trim().isEmpty()) {
                String searchPattern = "%" + name.toLowerCase() + "%";
                var reporterJoin = root.join("reporterUser", JoinType.LEFT);

                Predicate firstNameMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(reporterJoin.get("firstName")),
                    searchPattern
                );
                Predicate lastNameMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(reporterJoin.get("lastName")),
                    searchPattern
                );

                predicates.add(criteriaBuilder.or(firstNameMatch, lastNameMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);

        List<ViolationAdminItem> items = violations.getContent().stream()
                .map(violation -> {
                    User reportedUser = null;
                    Property reportedProperty = null;

                    if (violation.getRelatedEntityType() == Constants.ViolationReportedTypeEnum.PROPERTY) {
                        try {
                            reportedProperty = propertyService.findPropertyById(violation.getRelatedEntityId());
                        } catch (Exception e) {
                            log.warn("Property not found: {}", violation.getRelatedEntityId());
                        }
                    } else {
                        try {
                            reportedUser = userService.findById(violation.getRelatedEntityId());
                        } catch (Exception e) {
                            log.warn("User not found: {}", violation.getRelatedEntityId());
                        }
                    }

                    return violationMapper.toAdminItem(violation, reportedUser, reportedProperty);
                })
                .toList();

        return new PageImpl<>(items, pageable, violations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViolationUserItem> getMyViolationItems(Pageable pageable) {
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }

        Specification<ViolationReport> spec = (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("reporterUser").get("id"), currentUser.getId());

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);

        List<ViolationUserItem> items = violations.getContent().stream().map(violation -> {
            String targetName = getTargetName(violation);
            return violationMapper.toUserItem(violation, targetName);
        }).toList();

        return new PageImpl<>(items, pageable, violations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationUserDetails getViolationUserDetailsById(UUID id) {
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }

        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + id));

        // Check if current user is the reporter
        if (!violation.getReporterUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to view this violation");
        }

        String targetName = getTargetName(violation);
        return violationMapper.toUserDetails(violation, targetName);
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationAdminDetails getViolationAdminDetailsById(UUID id) {
        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + id));

        User reportedUser = null;
        Property reportedProperty = null;

        if (violation.getRelatedEntityType() == Constants.ViolationReportedTypeEnum.PROPERTY) {
            try {
                reportedProperty = propertyService.findPropertyById(violation.getRelatedEntityId());
            } catch (Exception e) {
                log.warn("Property not found: {}", violation.getRelatedEntityId());
            }
        } else {
            try {
                reportedUser = userService.findById(violation.getRelatedEntityId());
            } catch (Exception e) {
                log.warn("User not found: {}", violation.getRelatedEntityId());
            }
        }

        return violationMapper.toAdminDetails(violation, reportedUser, reportedProperty);
    }

    @Override
    @Transactional
    public ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles) {
        // Get current user as reporter
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }

        // Validate reported entity exists
        validateReportedEntity(request.getViolationReportedType(), request.getReportedId());

        // Create violation report
        ViolationReport violation = ViolationReport.builder()
                .reporterUser(currentUser)
                .relatedEntityType(request.getViolationReportedType())
                .relatedEntityId(request.getReportedId())
                .violationType(request.getViolationType())
                .description(request.getDescription())
                .status(Constants.ViolationStatusEnum.REPORTED)
                .penaltyApplied(null)
                .resolutionNotes(null)
                .resolvedAt(null)
                .mediaList(new ArrayList<>())
                .build();

        // Save to get ID
        ViolationReport savedViolation = violationRepository.save(violation);

        // Upload evidence files if provided
        if (evidenceFiles != null && evidenceFiles.length > 0) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String fileUrl = cloudinaryService.uploadFile(file, "violations/" + savedViolation.getId());
                        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

                        // Determine media type based on MIME type
                        Constants.MediaTypeEnum mediaType = Constants.MediaTypeEnum.IMAGE;
                        if (mimeType.startsWith("application/") || mimeType.startsWith("text/")) {
                            mediaType = Constants.MediaTypeEnum.DOCUMENT;
                        }

                        Media media = Media.builder()
                                .violationReport(savedViolation)
                                .mediaType(mediaType)
                                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getName())
                                .filePath(fileUrl)
                                .mimeType(mimeType)
                                .build();

                        savedViolation.getMediaList().add(media);
                    } catch (Exception e) {
                        log.error("Failed to upload evidence file for violation {}: {}", savedViolation.getId(), e.getMessage());
                        // Continue with other files
                    }
                }
            }
            violationRepository.save(savedViolation);
        }

        log.info("User {} created violation report {} for {} with ID {}",
                currentUser.getId(), savedViolation.getId(),
                request.getViolationReportedType(), request.getReportedId());

        String targetName = getTargetName(savedViolation);
        return violationMapper.toUserDetails(savedViolation, targetName);
    }

    @Override
    @Transactional
    public ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request) {
        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + id));

        // Update only allowed fields (admin can only update resolution-related fields)
        violation.setStatus(request.getStatus());

        if (request.getResolutionNotes() != null) {
            violation.setResolutionNotes(request.getResolutionNotes());
        }

        if (request.getPenaltyApplied() != null) {
            violation.setPenaltyApplied(request.getPenaltyApplied());
        }

        // Auto-set resolvedAt when status is RESOLVED
        if (request.getStatus() == Constants.ViolationStatusEnum.RESOLVED && violation.getResolvedAt() == null) {
            violation.setResolvedAt(LocalDateTime.now());
        }

        // Clear resolvedAt if status is changed from RESOLVED to something else
        if (request.getStatus() != Constants.ViolationStatusEnum.RESOLVED && violation.getResolvedAt() != null) {
            violation.setResolvedAt(null);
        }

        ViolationReport updatedViolation = violationRepository.save(violation);

        log.info("Admin updated violation report {} - Status: {}, Penalty: {}",
                id, request.getStatus(), request.getPenaltyApplied());

        // Fetch related entities for response
        User reportedUser = null;
        Property reportedProperty = null;

        if (updatedViolation.getRelatedEntityType() == Constants.ViolationReportedTypeEnum.PROPERTY) {
            try {
                reportedProperty = propertyService.findPropertyById(updatedViolation.getRelatedEntityId());
            } catch (Exception e) {
                log.warn("Property not found: {}", updatedViolation.getRelatedEntityId());
            }
        } else {
            try {
                reportedUser = userService.findById(updatedViolation.getRelatedEntityId());
            } catch (Exception e) {
                log.warn("User not found: {}", updatedViolation.getRelatedEntityId());
            }
        }

        return violationMapper.toAdminDetails(updatedViolation, reportedUser, reportedProperty);
    }

    private void validateReportedEntity(Constants.ViolationReportedTypeEnum reportedType, UUID reportedId) {
        if (reportedType == Constants.ViolationReportedTypeEnum.PROPERTY) {
            // Validate property exists
            propertyService.findPropertyById(reportedId);
        } else {
            // Validate user exists (for CUSTOMER, PROPERTY_OWNER, SALES_AGENT)
            userService.findById(reportedId);
        }
    }

    private String getTargetName(ViolationReport violation) {
        if (violation.getRelatedEntityType() == Constants.ViolationReportedTypeEnum.PROPERTY) {
            try {
                Property property = propertyService.findPropertyById(violation.getRelatedEntityId());
                return property != null ? property.getTitle() : "Unknown Property";
            } catch (Exception e) {
                return "Unknown Property";
            }
        } else {
            try {
                User user = userService.findById(violation.getRelatedEntityId());
                return user != null ? (user.getFirstName() + " " + user.getLastName()) : "Unknown User";
            } catch (Exception e) {
                return "Unknown User";
            }
        }
    }
}
