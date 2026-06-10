package microservices.moderationservice.moderation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import com.se.bds.common.enums.*;
import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.event.ViolationPenaltyAppliedEvent;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.dto.request.UpdateViolationRequest;
import microservices.moderationservice.moderation.dto.request.ViolationCreateRequest;
import microservices.moderationservice.moderation.dto.response.*;
import microservices.moderationservice.moderation.entity.replica.PropertyReplica;
import microservices.moderationservice.moderation.entity.replica.UserReplica;
import microservices.moderationservice.moderation.entity.ViolationReport;
import microservices.moderationservice.moderation.entity.ViolationEvidence;
import microservices.moderationservice.moderation.mapper.ViolationMapper;
import microservices.moderationservice.moderation.repository.ViolationRepository;
import microservices.moderationservice.moderation.repository.mongo.ViolationReportDetailsRepository;
import microservices.moderationservice.moderation.repository.replica.PropertyReplicaRepository;
import microservices.moderationservice.moderation.repository.replica.UserReplicaRepository;
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
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

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
        Specification<ViolationReport> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (violationTypes != null && !violationTypes.isEmpty()) {
                predicates.add(root.get("violationType").in(violationTypes));
            }

            if (violationStatusEnums != null && !violationStatusEnums.isEmpty()) {
                predicates.add(root.get("status").in(violationStatusEnums));
            }

            if (month != null && year != null) {
                LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
                LocalDateTime endDate = startDate.plusMonths(1);
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startDate, endDate));
            } else if (year != null) {
                LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
                LocalDateTime endDate = startDate.plusYears(1);
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startDate, endDate));
            }

            if (name != null && !name.trim().isEmpty()) {
                log.debug("Name filter is currently skipped in moderation service decoupling phase");
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);
        ReplicaLookup replicaLookup = loadReplicaLookup(violations.getContent());
        List<ViolationAdminItem> items = violations.getContent().stream()
                .map(v -> {
                    ViolationAdminItem item = violationMapper.toAdminItem(v);
                    applyReporterInfo(replicaLookup.users(), v.getReporterId(), item::setReporterName, item::setReporterAvatarUrl);
                    applyReportedInfo(replicaLookup, v.getRelatedEntityId(), v.getRelatedEntityType(),
                            item::setReportedName, item::setReportedAvatarUrl, null);
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

        Specification<ViolationReport> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("reporterId"), currentUserId);

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);
        ReplicaLookup replicaLookup = loadReplicaLookup(violations.getContent());
        List<ViolationUserItem> items = violations.getContent().stream()
                .map(v -> {
                    ViolationUserItem item = violationMapper.toUserItem(v);
                    applyReporterInfo(replicaLookup.users(), v.getReporterId(), item::setReporterName, item::setReporterAvatarUrl);
                    applyReportedInfo(replicaLookup, v.getRelatedEntityId(), v.getRelatedEntityType(),
                            item::setReportedName, item::setReportedAvatarUrl, null);
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

        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("VIOLATION_NOT_FOUND", "Violation not found: " + id));

        if (!currentUserId.equals(violation.getReporterId())) {
            throw new IllegalStateException("You are not authorized to view this violation");
        }

        ViolationUserDetails details = violationMapper.toUserDetails(violation);
        ReplicaLookup replicaLookup = loadReplicaLookup(List.of(violation));
        applyReporterInfo(replicaLookup.users(), violation.getReporterId(), details::setReporterName, details::setReporterAvatarUrl);
        applyReportedInfo(replicaLookup, violation.getRelatedEntityId(), violation.getRelatedEntityType(),
                details::setReportedName, details::setReportedAvatarUrl, null);
        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationAdminDetails getViolationAdminDetailsById(UUID id) {
        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("VIOLATION_NOT_FOUND", "Violation not found: " + id));

        ViolationAdminDetails details = violationMapper.toAdminDetails(violation);
        ReplicaLookup replicaLookup = loadReplicaLookup(List.of(violation));
        applyReporterInfo(replicaLookup.users(), violation.getReporterId(), details::setReporterName, details::setReporterAvatarUrl);
        applyReportedInfo(replicaLookup, violation.getRelatedEntityId(), violation.getRelatedEntityType(),
                details::setReportedName, details::setReportedAvatarUrl, d -> {
            details.setReportedRole(d.get("role"));
            details.setReportedEmail(d.get("email"));
            details.setReportedPhoneNumber(d.get("phoneNumber"));
            details.setReportedTitle(d.get("title"));
        });
        return details;
    }

    @Override
    @Transactional
    public ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles) {
        // Validate reported entity ID using Fail-Secure policy
        validateReportedEntity(request.getReportedId(), request.getViolationReportedType());

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

        if (evidenceFiles != null && evidenceFiles.length > 0) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String fileUrl = fileStorageService.uploadFile(file, "violations/" + savedViolation.getId());
                        String mimeType = file.getContentType();
                        MediaTypeEnum mediaType = (mimeType != null && mimeType.startsWith("image/"))
                                ? MediaTypeEnum.IMAGE : MediaTypeEnum.DOCUMENT;
                        ViolationEvidence evidence = ViolationEvidence.builder()
                                .fileUrl(fileUrl)
                                .mediaType(mediaType)
                                .fileName(file.getOriginalFilename())
                                .mimeType(mimeType)
                                .build();
                        savedViolation.getEvidenceList().add(evidence);
                    } catch (Exception e) {
                        log.error("Failed to upload evidence file for violation {}: {}", savedViolation.getId(), e.getMessage());
                    }
                }
            }
            violationRepository.save(savedViolation);
        }

        log.info("Reporter {} created violation report {} for {} with ID {}",
                savedViolation.getReporterId(), savedViolation.getId(),
                request.getViolationReportedType(), request.getReportedId());

        return getViolationUserDetailsById(savedViolation.getId());
    }

    @Override
    @Transactional
    public ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request) {
        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("VIOLATION_NOT_FOUND", "Violation not found: " + id));

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
            // Publish penalty event if penalty is applied and is not WARNING
            if (violation.getPenaltyApplied() != null && violation.getPenaltyApplied() != PenaltyAppliedEnum.WARNING) {
                publishPenaltyAppliedEvent(violation);
            }
        } else {
            violation.setResolvedAt(null);
        }

        ViolationReport updatedViolation = violationRepository.save(violation);

        log.info("Admin updated violation report {} - Status: {}, Penalty: {}",
                id, request.getStatus(), request.getPenaltyApplied());

        return getViolationAdminDetailsById(updatedViolation.getId());
    }

    @Override
    public ViolationReportStats getViolationStats(int year) {
        int month;
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        if (year > currentYear) return null;

        if (currentYear == year) {
            month = currentMonth;
            violationReportScheduler.initViolationReportData(month, year).join();
        } else {
            month = 12;
        }

        ViolationReportDetails violationReport = violationReportDetailsRepository
                .findFirstByBaseReportData_MonthAndBaseReportData_YearOrderByCreatedAtDesc(month, year);

        if (violationReport == null) {
            log.warn("No ViolationReportDetails found for year {} and month {}", year, month);
            return null;
        }

        List<ViolationReportDetails> violationReportList = violationReportDetailsRepository.findAllByBaseReportData_Year(year);

        Map<Integer, Integer> totalViolationReportChart = new HashMap<>();
        Map<Integer, Integer> accountsSuspendedChart = new HashMap<>();
        Map<Integer, Integer> propertiesRemovedChart = new HashMap<>();
        Map<String, Map<Integer, Long>> violationTrends = new HashMap<>();

        for (ViolationReportDetails reportItem : violationReportList) {
            int monthI = reportItem.getBaseReportData().getMonth();

            totalViolationReportChart.put(monthI, reportItem.getTotalViolationReports() != null
                    ? reportItem.getTotalViolationReports() : 0);
            accountsSuspendedChart.put(monthI, reportItem.getAccountsSuspended() != null
                    ? reportItem.getAccountsSuspended() : 0);
            propertiesRemovedChart.put(monthI, reportItem.getPropertiesRemoved() != null
                    ? reportItem.getPropertiesRemoved() : 0);

            if (reportItem.getViolationTypeCounts() != null) {
                for (Map.Entry<String, Integer> entry : reportItem.getViolationTypeCounts().entrySet()) {
                    String typeName = entry.getKey();
                    violationTrends.computeIfAbsent(typeName, k -> new HashMap<>())
                            .put(monthI, entry.getValue().longValue());
                }
            }
        }

        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        int newThisMonth = violationRepository.countByCreatedAtBetween(startOfMonth, startOfNextMonth);
        int pendingCount = violationRepository.countByStatus(ViolationStatusEnum.PENDING);
        int underReviewCount = violationRepository.countByStatus(ViolationStatusEnum.UNDER_REVIEW);

        return ViolationReportStats.builder()
                .totalViolationReports(violationReport.getTotalViolationReports())
                .newThisMonth(newThisMonth)
                .unsolved(pendingCount + underReviewCount)
                .avgResolutionTimeHours(violationReport.getAvgResolutionTimeHours() != null
                        ? violationReport.getAvgResolutionTimeHours().doubleValue() : 0.0)
                .totalViolationReportChart(totalViolationReportChart)
                .violationTrends(violationTrends)
                .accountsSuspendedChart(accountsSuspendedChart)
                .propertiesRemovedChart(propertiesRemovedChart)
                .build();
    }

    private void validateReportedEntity(UUID reportedId, ViolationReportedTypeEnum reportedType) {
        if (reportedType == ViolationReportedTypeEnum.PROPERTY) {
            boolean propertyExists = propertyReplicaRepository.findById(reportedId)
                    .filter(property -> property.getStatus() == null || !"DELETED".equalsIgnoreCase(property.getStatus()))
                    .isPresent();
            if (!propertyExists) {
                throw new BusinessException("PROPERTY_NOT_FOUND", "Reported property not found: " + reportedId);
            }
        } else {
            String roleName = switch (reportedType) {
                case CUSTOMER -> "CUSTOMER";
                case SALES_AGENT -> "SALESAGENT";
                case PROPERTY_OWNER -> "PROPERTY_OWNER";
                default -> throw new BusinessException("UNSUPPORTED_ENTITY_TYPE", "Unsupported reported entity type: " + reportedType);
            };
            boolean userExists = userReplicaRepository.findById(reportedId)
                    .filter(user -> Boolean.TRUE.equals(user.getActive()))
                    .filter(user -> user.getRole() != null && user.getRole().equalsIgnoreCase(roleName))
                    .isPresent();
            if (!userExists) {
                throw new BusinessException("USER_NOT_FOUND", "Reported user not found or inactive: " + reportedId);
            }
        }
    }

    private ReplicaLookup loadReplicaLookup(List<ViolationReport> violations) {
        Set<UUID> userIds = new HashSet<>();
        Set<UUID> propertyIds = new HashSet<>();

        for (ViolationReport violation : violations) {
            if (violation.getReporterId() != null) {
                userIds.add(violation.getReporterId());
            }
            if (violation.getRelatedEntityId() == null) {
                continue;
            }
            if (violation.getRelatedEntityType() == ViolationReportedTypeEnum.PROPERTY) {
                propertyIds.add(violation.getRelatedEntityId());
            } else {
                userIds.add(violation.getRelatedEntityId());
            }
        }

        Map<UUID, UserReplica> users = new HashMap<>();
        userReplicaRepository.findAllById(userIds).forEach(user -> users.put(user.getId(), user));

        Map<UUID, PropertyReplica> properties = new HashMap<>();
        propertyReplicaRepository.findAllById(propertyIds).forEach(property -> properties.put(property.getId(), property));

        return new ReplicaLookup(users, properties);
    }

    private void applyReporterInfo(
            Map<UUID, UserReplica> users,
            UUID reporterId,
            Consumer<String> nameSetter,
            Consumer<String> avatarSetter
    ) {
        UserReplica user = reporterId != null ? users.get(reporterId) : null;
        nameSetter.accept(user != null && user.getFullName() != null ? user.getFullName() : "Unknown User");
        avatarSetter.accept(user != null ? user.getAvatarUrl() : null);
    }

    private void applyReportedInfo(
            ReplicaLookup replicaLookup,
            UUID reportedId,
            ViolationReportedTypeEnum reportedType,
            Consumer<String> nameSetter,
            Consumer<String> avatarSetter,
            Consumer<Map<String, String>> detailsSetter
    ) {
        if (reportedId == null) {
            return;
        }
        if (reportedType == ViolationReportedTypeEnum.PROPERTY) {
            PropertyReplica property = replicaLookup.properties().get(reportedId);
            nameSetter.accept(property != null && property.getTitle() != null ? property.getTitle() : "Unknown Property");
            avatarSetter.accept(property != null ? property.getThumbnailUrl() : null);
            if (detailsSetter != null) {
                Map<String, String> details = new HashMap<>();
                details.put("title", property != null ? property.getTitle() : null);
                detailsSetter.accept(details);
            }
        } else {
            UserReplica user = replicaLookup.users().get(reportedId);
            nameSetter.accept(user != null && user.getFullName() != null ? user.getFullName() : "Unknown User");
            avatarSetter.accept(user != null ? user.getAvatarUrl() : null);
            if (detailsSetter != null) {
                Map<String, String> details = new HashMap<>();
                details.put("role", user != null && user.getRole() != null ? user.getRole() : "");
                details.put("email", user != null && user.getEmail() != null ? user.getEmail() : "");
                details.put("phoneNumber", user != null && user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                detailsSetter.accept(details);
            }
        }
    }

    private void publishPenaltyAppliedEvent(ViolationReport violation) {
        try {
            UUID reportedUserId = null;
            if (violation.getRelatedEntityType() == ViolationReportedTypeEnum.PROPERTY) {
                reportedUserId = propertyReplicaRepository.findById(violation.getRelatedEntityId())
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
            log.info("Successfully published ViolationPenaltyAppliedEvent to Kafka: {}", payload);
        } catch (Exception ex) {
            log.error("Failed to publish ViolationPenaltyAppliedEvent to Kafka", ex);
        }
    }

    private record ReplicaLookup(
            Map<UUID, UserReplica> users,
            Map<UUID, PropertyReplica> properties
    ) {
    }

    private UUID resolveReporterId(ViolationCreateRequest request) {
        if (request.getReporterId() != null) {
            return request.getReporterId();
        }
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        throw new BusinessException("REPORTER_ID_REQUIRED", "Reporter ID is required");
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException ex) {
            log.debug("Could not parse authenticated principal into UUID: {}", principalName);
            return null;
        }
    }
}
