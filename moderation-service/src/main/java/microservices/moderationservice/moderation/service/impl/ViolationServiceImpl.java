package microservices.moderationservice.moderation.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.common.Constants;
import microservices.moderationservice.common.exception.NotFoundException;
import microservices.moderationservice.moderation.dto.request.UpdateViolationRequest;
import microservices.moderationservice.moderation.dto.request.ViolationCreateRequest;
import microservices.moderationservice.moderation.dto.response.ViolationAdminDetails;
import microservices.moderationservice.moderation.dto.response.ViolationAdminItem;
import microservices.moderationservice.moderation.dto.response.ViolationUserDetails;
import microservices.moderationservice.moderation.dto.response.ViolationUserItem;
import microservices.moderationservice.moderation.entity.ViolationReport;
import microservices.moderationservice.moderation.mapper.ViolationMapper;
import microservices.moderationservice.moderation.repository.ViolationRepository;
import microservices.moderationservice.moderation.service.ViolationService;
import microservices.moderationservice.storage.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ViolationMapper violationMapper;
    private final FileStorageService fileStorageService;

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
                // TODO: Phase 2 - Call Core Service via HTTP to resolve/filter by reporter name.
                log.debug("Name filter is currently skipped in moderation service decoupling phase");
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);
        List<ViolationAdminItem> items = violations.getContent().stream()
                .map(violationMapper::toAdminItem)
                .toList();

        return new PageImpl<>(items, pageable, violations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViolationUserItem> getMyViolationItems(Pageable pageable) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new NotFoundException("Current user not found");
        }

        Specification<ViolationReport> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("reporterId"), currentUserId);

        Page<ViolationReport> violations = violationRepository.findAll(spec, pageable);
        List<ViolationUserItem> items = violations.getContent().stream()
                .map(violationMapper::toUserItem)
                .toList();

        return new PageImpl<>(items, pageable, violations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationUserDetails getViolationUserDetailsById(UUID id) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new NotFoundException("Current user not found");
        }

        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + id));

        if (!currentUserId.equals(violation.getReporterId())) {
            throw new IllegalStateException("You are not authorized to view this violation");
        }

        return violationMapper.toUserDetails(violation);
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationAdminDetails getViolationAdminDetailsById(UUID id) {
        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + id));

        return violationMapper.toAdminDetails(violation);
    }

    @Override
    @Transactional
    public ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles) {
        // TODO: Phase 2 - Call Core Service via HTTP to validate ID
        ViolationReport violation = ViolationReport.builder()
                .reporterId(resolveReporterId(request))
                .relatedEntityType(request.getViolationReportedType())
                .relatedEntityId(request.getReportedId())
                .violationType(request.getViolationType())
                .description(request.getDescription())
                .status(Constants.ViolationStatusEnum.REPORTED)
                .penaltyApplied(null)
                .resolutionNotes(null)
                .resolvedAt(null)
                .evidenceUrls(new ArrayList<>())
                .build();

        ViolationReport savedViolation = violationRepository.save(violation);

        if (evidenceFiles != null && evidenceFiles.length > 0) {
            for (MultipartFile file : evidenceFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String fileUrl = fileStorageService.uploadFile(file, "violations/" + savedViolation.getId());
                        savedViolation.getEvidenceUrls().add(fileUrl);
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

        return violationMapper.toUserDetails(savedViolation);
    }

    @Override
    @Transactional
    public ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request) {
        ViolationReport violation = violationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + id));

        violation.setStatus(request.getStatus());

        if (request.getResolutionNotes() != null) {
            violation.setResolutionNotes(request.getResolutionNotes());
        }

        if (request.getPenaltyApplied() != null) {
            violation.setPenaltyApplied(request.getPenaltyApplied());
        }

        if (request.getStatus() == Constants.ViolationStatusEnum.RESOLVED && violation.getResolvedAt() == null) {
            violation.setResolvedAt(LocalDateTime.now());
        }

        if (request.getStatus() != Constants.ViolationStatusEnum.RESOLVED && violation.getResolvedAt() != null) {
            violation.setResolvedAt(null);
        }

        ViolationReport updatedViolation = violationRepository.save(violation);

        log.info("Admin updated violation report {} - Status: {}, Penalty: {}",
                id, request.getStatus(), request.getPenaltyApplied());

        return violationMapper.toAdminDetails(updatedViolation);
    }

    private UUID resolveReporterId(ViolationCreateRequest request) {
        if (request.getReporterId() != null) {
            return request.getReporterId();
        }
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        throw new NotFoundException("Reporter ID is required");
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
