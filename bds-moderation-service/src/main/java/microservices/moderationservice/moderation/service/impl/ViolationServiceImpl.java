package microservices.moderationservice.moderation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.*;
import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.event.ViolationPenaltyAppliedEvent;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.dto.request.UpdateViolationRequest;
import microservices.moderationservice.moderation.dto.request.ViolationCreateRequest;
import microservices.moderationservice.moderation.dto.response.*;
import microservices.moderationservice.moderation.entity.PropertyReplica;
import microservices.moderationservice.moderation.entity.UserReplica;
import microservices.moderationservice.moderation.entity.ViolationReport;
import microservices.moderationservice.moderation.entity.ViolationEvidence;
import microservices.moderationservice.moderation.mapper.ViolationMapper;
import microservices.moderationservice.moderation.repository.PropertyReplicaRepository;
import microservices.moderationservice.moderation.repository.UserReplicaRepository;
import microservices.moderationservice.moderation.repository.ViolationRepository;
import microservices.moderationservice.moderation.repository.mongo.ViolationReportDetailsRepository;
import microservices.moderationservice.moderation.scheduler.ViolationReportScheduler;
import microservices.moderationservice.moderation.schema.ViolationReportDetails;
import microservices.moderationservice.moderation.service.ViolationService;
import microservices.moderationservice.storage.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * REFACTORED: loại bỏ hoàn toàn CoreServiceClient và IamServiceClient.
 *
 * Thay thế:
 * - enrichReporterInfo  → query UserReplicaRepository (local DB, không Feign)
 * - enrichReportedInfo  → query PropertyReplicaRepository / UserReplicaRepository
 * - validateReportedEntity → query local replica thay vì gọi Feign
 * - publishPenaltyAppliedEvent → lấy ownerId từ PropertyReplica thay vì Feign
 *
 * Kết quả: 0 lời gọi mạng trong tất cả các method - chỉ local DB queries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    private final ViolationRepository violationRepository;
    private final ViolationMapper violationMapper;
    private final FileStorageService fileStorageService;
    private final PropertyReplicaRepository propertyReplicaRepository;
    private final UserReplicaRepository userReplicaRepository;
    private final ViolationReportDetailsRepository violationReportDetailsRepository;
    private final ViolationReportScheduler violationReportScheduler;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<ViolationAdminItem> getAdminViolationItems(
            Pageable pageable,
            List<ViolationTypeEnum> violationTypes,
            List<ViolationStatusEnum> violationStatusEnums,
            String name,
            Integer month,
            Integer year
    ) {
        Specification<ViolationReport> spec = buildSpec(violationTypes, violationStatusEnums, month, year);
        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);

        List<ViolationAdminItem> items = violations.getContent().stream()
                .map(v -> {
                    ViolationAdminItem item = violationMapper.toAdminItem(v);
                    enrichReporterInfo(v.getReporterId(), item);
                    enrichReportedInfoForAdminItem(v.getRelatedEntityId(), v.getRelatedEntityType(), item);
                    return item;
                })
                .toList();

        return new PageImpl<>(items, pageable, violations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViolationUserItem> getMyViolationItems(Pageable pageable) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("USER_NOT_FOUND", "Current user not found");
        }

        Specification<ViolationReport> spec = (root, query, cb) ->
                cb.equal(root.get("reporterId"), currentUserId);

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);
        List<ViolationUserItem> items = violations.getContent().stream()
                .map(v -> {
                    ViolationUserItem item = violationMapper.toUserItem(v);
                    enrichReporterInfo(v.getReporterId(), item);
                    enrichReportedInfoForUserItem(v.getRelatedEntityId(), v.getRelatedEntityType(), item);
                    return item;
                })
                .toList();

        return new PageImpl<>(items, pageable, violations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationUserDetails getViolationUserDetailsById(UUID id) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("USER_NOT_FOUND", "Current user not found");
        }
        ViolationReport violation = findViolationOrThrow(id);
        if (!currentUserId.equals(violation.getReporterId())) {
            throw new IllegalStateException("You are not authorized to view this violation");
        }
        return buildUserDetails(violation);
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationAdminDetails getViolationAdminDetailsById(UUID id) {
        ViolationReport violation = findViolationOrThrow(id);
        return buildAdminDetails(violation);
    }

    @Override
    @Transactional
    public ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles) {
        // Validate using local replica - KHÔNG gọi Feign
        validateReportedEntityFromReplica(request.getReportedId(), request.getViolationReportedType());

        ViolationReport violation = ViolationReport.builder()
                .reporterId(resolveReporterId(request))
                .relatedEntityType(request.getViolationReportedType())
                .relatedEntityId(request.getReportedId())
                .violationType(request.getViolationType())
                .description(request.getDescription())
                .status(ViolationStatusEnum.REPORTED)
                .penaltyApplied(null)
                .resolutionNotes(null)
                .resolvedAt(null)
                .evidenceList(new ArrayList<>())
                .build();

        ViolationReport savedViolation = violationRepository.save(violation);

        if (evidenceFiles != null) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String fileUrl = fileStorageService.uploadFile(file, "violations/" + savedViolation.getId());
                        String mimeType = file.getContentType();
                        MediaTypeEnum mediaType = (mimeType != null && mimeType.startsWith("image/"))
                                ? MediaTypeEnum.IMAGE : MediaTypeEnum.DOCUMENT;
                        savedViolation.getEvidenceList().add(ViolationEvidence.builder()
                                .fileUrl(fileUrl)
                                .mediaType(mediaType)
                                .fileName(file.getOriginalFilename())
                                .mimeType(mimeType)
                                .build());
                    } catch (Exception e) {
                        log.error("Failed to upload evidence file for violation {}", savedViolation.getId(), e);
                    }
                }
            }
            violationRepository.save(savedViolation);
        }

        log.info("Reporter {} created violation report {} for {} with ID {}",
                savedViolation.getReporterId(), savedViolation.getId(),
                request.getViolationReportedType(), request.getReportedId());

        return buildUserDetails(savedViolation);
    }

    @Override
    @Transactional
    public ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request) {
        ViolationReport violation = findViolationOrThrow(id);
        violation.setStatus(request.getStatus());

        if (request.getResolutionNotes() != null) {
            violation.setResolutionNotes(request.getResolutionNotes());
        }
        if (request.getPenaltyApplied() != null) {
            violation.setPenaltyApplied(request.getPenaltyApplied());
        }
        if (request.getStatus() == ViolationStatusEnum.RESOLVED) {
            if (violation.getResolvedAt() == null) {
                violation.setResolvedAt(LocalDateTime.now());
            }
            if (violation.getPenaltyApplied() != null && violation.getPenaltyApplied() != PenaltyAppliedEnum.WARNING) {
                publishPenaltyAppliedEvent(violation);
            }
        } else {
            violation.setResolvedAt(null);
        }

        ViolationReport updated = violationRepository.save(violation);
        log.info("Admin updated violation report {} - Status: {}, Penalty: {}",
                id, request.getStatus(), request.getPenaltyApplied());

        return buildAdminDetails(updated);
    }

    @Override
    public ViolationReportStats getViolationStats(int year) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        if (year > currentYear) return null;

        int month = (currentYear == year) ? currentMonth : 12;
        if (currentYear == year) {
            violationReportScheduler.initViolationReportData(month, year).join();
        }

        ViolationReportDetails violationReport = violationReportDetailsRepository
                .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(month, year);
        if (violationReport == null) {
            log.warn("No ViolationReportDetails found for year {} and month {}", year, month);
            return null;
        }

        List<ViolationReportDetails> allReports = violationReportDetailsRepository.findAllByBaseReportData_Year(year);

        Map<Integer, Integer> totalChart = new HashMap<>();
        Map<Integer, Integer> suspendedChart = new HashMap<>();
        Map<Integer, Integer> removedChart = new HashMap<>();
        Map<String, Map<Integer, Long>> violationTrends = new HashMap<>();

        for (ViolationReportDetails r : allReports) {
            int m = r.getBaseReportData().getMonth();
            totalChart.put(m, r.getTotalViolationReports() != null ? r.getTotalViolationReports() : 0);
            suspendedChart.put(m, r.getAccountsSuspended() != null ? r.getAccountsSuspended() : 0);
            removedChart.put(m, r.getPropertiesRemoved() != null ? r.getPropertiesRemoved() : 0);
            if (r.getViolationTypeCounts() != null) {
                r.getViolationTypeCounts().forEach((type, count) ->
                        violationTrends.computeIfAbsent(type, k -> new HashMap<>()).put(m, count.longValue()));
            }
        }

        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        int newThisMonth = violationRepository.countByCreatedAtBetween(startOfMonth, startOfMonth.plusMonths(1));
        int pendingCount = violationRepository.countByStatus(ViolationStatusEnum.PENDING);
        int underReviewCount = violationRepository.countByStatus(ViolationStatusEnum.UNDER_REVIEW);

        return ViolationReportStats.builder()
                .totalViolationReports(violationReport.getTotalViolationReports())
                .newThisMonth(newThisMonth)
                .unsolved(pendingCount + underReviewCount)
                .avgResolutionTimeHours(violationReport.getAvgResolutionTimeHours() != null
                        ? violationReport.getAvgResolutionTimeHours().doubleValue() : 0.0)
                .totalViolationReportChart(totalChart)
                .violationTrends(violationTrends)
                .accountsSuspendedChart(suspendedChart)
                .propertiesRemovedChart(removedChart)
                .build();
    }

    // ======================== HELPERS ========================

    private void validateReportedEntityFromReplica(UUID reportedId, ViolationReportedTypeEnum reportedType) {
        if (reportedType == ViolationReportedTypeEnum.PROPERTY) {
            boolean exists = propertyReplicaRepository.findByPropertyIdAndDeletedFalse(reportedId).isPresent();
            if (!exists) {
                throw new BusinessException("PROPERTY_NOT_FOUND",
                        "Reported property not found in local replica: " + reportedId);
            }
        } else {
            boolean exists = userReplicaRepository.findByUserIdAndActiveTrue(reportedId).isPresent();
            if (!exists) {
                throw new BusinessException("USER_NOT_FOUND",
                        "Reported user not found or inactive in local replica: " + reportedId);
            }
        }
    }

    private void enrichReporterInfo(UUID reporterId, ViolationAdminItem item) {
        if (reporterId == null) return;
        userReplicaRepository.findById(reporterId).ifPresentOrElse(
                user -> {
                    item.setReporterName(user.getFullName());
                    item.setReporterAvatarUrl(user.getAvatarUrl());
                },
                () -> {
                    item.setReporterName("Unknown User");
                    item.setReporterAvatarUrl(null);
                }
        );
    }

    private void enrichReporterInfo(UUID reporterId, ViolationUserItem item) {
        if (reporterId == null) return;
        userReplicaRepository.findById(reporterId).ifPresentOrElse(
                user -> {
                    item.setReporterName(user.getFullName());
                    item.setReporterAvatarUrl(user.getAvatarUrl());
                },
                () -> {
                    item.setReporterName("Unknown User");
                    item.setReporterAvatarUrl(null);
                }
        );
    }

    private void enrichReportedInfoForAdminItem(UUID reportedId, ViolationReportedTypeEnum reportedType,
                                                ViolationAdminItem item) {
        if (reportedId == null) return;
        if (reportedType == ViolationReportedTypeEnum.PROPERTY) {
            propertyReplicaRepository.findById(reportedId).ifPresentOrElse(
                    p -> {
                        item.setReportedName(p.getTitle() != null ? p.getTitle() : "Unknown Property");
                        item.setReportedAvatarUrl(p.getThumbnailUrl());
                    },
                    () -> {
                        item.setReportedName("Unknown Property");
                        item.setReportedAvatarUrl(null);
                    }
            );
        } else {
            userReplicaRepository.findById(reportedId).ifPresentOrElse(
                    u -> {
                        item.setReportedName(u.getFullName());
                        item.setReportedAvatarUrl(u.getAvatarUrl());
                    },
                    () -> {
                        item.setReportedName("Unknown User");
                        item.setReportedAvatarUrl(null);
                    }
            );
        }
    }

    private void enrichReportedInfoForUserItem(UUID reportedId, ViolationReportedTypeEnum reportedType,
                                               ViolationUserItem item) {
        if (reportedId == null) return;
        if (reportedType == ViolationReportedTypeEnum.PROPERTY) {
            propertyReplicaRepository.findById(reportedId).ifPresentOrElse(
                    p -> {
                        item.setReportedName(p.getTitle() != null ? p.getTitle() : "Unknown Property");
                        item.setReportedAvatarUrl(p.getThumbnailUrl());
                    },
                    () -> {
                        item.setReportedName("Unknown Property");
                        item.setReportedAvatarUrl(null);
                    }
            );
        } else {
            userReplicaRepository.findById(reportedId).ifPresentOrElse(
                    u -> {
                        item.setReportedName(u.getFullName());
                        item.setReportedAvatarUrl(u.getAvatarUrl());
                    },
                    () -> {
                        item.setReportedName("Unknown User");
                        item.setReportedAvatarUrl(null);
                    }
            );
        }
    }

    private ViolationUserDetails buildUserDetails(ViolationReport violation) {
        ViolationUserDetails details = violationMapper.toUserDetails(violation);
        userReplicaRepository.findById(violation.getReporterId()).ifPresent(u -> {
            details.setReporterName(u.getFullName());
            details.setReporterAvatarUrl(u.getAvatarUrl());
        });
        fillReportedIntoUserDetails(violation, details);
        return details;
    }

    private ViolationAdminDetails buildAdminDetails(ViolationReport violation) {
        ViolationAdminDetails details = violationMapper.toAdminDetails(violation);
        userReplicaRepository.findById(violation.getReporterId()).ifPresent(u -> {
            details.setReporterName(u.getFullName());
            details.setReporterAvatarUrl(u.getAvatarUrl());
        });
        fillReportedIntoAdminDetails(violation, details);
        return details;
    }

    private void fillReportedIntoUserDetails(ViolationReport violation, ViolationUserDetails details) {
        UUID reportedId = violation.getRelatedEntityId();
        if (reportedId == null) return;
        if (violation.getRelatedEntityType() == ViolationReportedTypeEnum.PROPERTY) {
            propertyReplicaRepository.findById(reportedId).ifPresent(p -> {
                details.setReportedName(p.getTitle() != null ? p.getTitle() : "Unknown Property");
                details.setReportedAvatarUrl(p.getThumbnailUrl());
            });
        } else {
            userReplicaRepository.findById(reportedId).ifPresent(u -> {
                details.setReportedName(u.getFullName());
                details.setReportedAvatarUrl(u.getAvatarUrl());
            });
        }
    }

    private void fillReportedIntoAdminDetails(ViolationReport violation, ViolationAdminDetails details) {
        UUID reportedId = violation.getRelatedEntityId();
        if (reportedId == null) return;
        if (violation.getRelatedEntityType() == ViolationReportedTypeEnum.PROPERTY) {
            propertyReplicaRepository.findById(reportedId).ifPresent(p -> {
                details.setReportedName(p.getTitle() != null ? p.getTitle() : "Unknown Property");
                details.setReportedAvatarUrl(p.getThumbnailUrl());
                details.setReportedTitle(p.getTitle());
            });
        } else {
            userReplicaRepository.findById(reportedId).ifPresent(u -> {
                details.setReportedName(u.getFullName());
                details.setReportedAvatarUrl(u.getAvatarUrl());
                details.setReportedRole(u.getRole());
                details.setReportedEmail(u.getEmail());
                details.setReportedPhoneNumber(u.getPhoneNumber());
            });
        }
    }

    private void publishPenaltyAppliedEvent(ViolationReport violation) {
        try {
            UUID reportedUserId = null;
            if (violation.getRelatedEntityType() == ViolationReportedTypeEnum.PROPERTY) {
                reportedUserId = propertyReplicaRepository
                        .findById(violation.getRelatedEntityId())
                        .map(PropertyReplica::getOwnerId)
                        .orElse(null);
            } else {
                reportedUserId = violation.getRelatedEntityId();
            }

            ViolationPenaltyAppliedEvent event = new ViolationPenaltyAppliedEvent(
                    violation.getId(),
                    reportedUserId,
                    violation.getReporterId(),
                    violation.getRelatedEntityId(),
                    violation.getRelatedEntityType().name(),
                    violation.getViolationType().name(),
                    violation.getPenaltyApplied().name(),
                    Instant.now()
            );

            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("violation-penalty-applied", violation.getRelatedEntityId().toString(), payload);
            log.info("Published ViolationPenaltyAppliedEvent: violationId={}", violation.getId());
        } catch (Exception ex) {
            log.error("Failed to publish ViolationPenaltyAppliedEvent", ex);
        }
    }

    private ViolationReport findViolationOrThrow(UUID id) {
        return violationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("VIOLATION_NOT_FOUND", "Violation not found: " + id));
    }

    private Specification<ViolationReport> buildSpec(List<ViolationTypeEnum> types,
                                                     List<ViolationStatusEnum> statuses,
                                                     Integer month, Integer year) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (types != null && !types.isEmpty()) predicates.add(root.get("violationType").in(types));
            if (statuses != null && !statuses.isEmpty()) predicates.add(root.get("status").in(statuses));
            if (month != null && year != null) {
                LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
                predicates.add(cb.between(root.get("createdAt"), start, start.plusMonths(1)));
            } else if (year != null) {
                LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
                predicates.add(cb.between(root.get("createdAt"), start, start.plusYears(1)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UUID resolveReporterId(ViolationCreateRequest request) {
        if (request.getReporterId() != null) return request.getReporterId();
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) return currentUserId;
        throw new BusinessException("REPORTER_ID_REQUIRED", "Reporter ID is required");
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        if (name == null || name.isBlank()) return null;
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}