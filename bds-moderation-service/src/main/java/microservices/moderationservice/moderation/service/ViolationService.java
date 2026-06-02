package microservices.moderationservice.moderation.service;

import microservices.moderationservice.common.Constants;
import microservices.moderationservice.moderation.dto.request.UpdateViolationRequest;
import microservices.moderationservice.moderation.dto.request.ViolationCreateRequest;
import microservices.moderationservice.moderation.dto.response.ViolationAdminDetails;
import microservices.moderationservice.moderation.dto.response.ViolationAdminItem;
import microservices.moderationservice.moderation.dto.response.ViolationUserDetails;
import microservices.moderationservice.moderation.dto.response.ViolationUserItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ViolationService {
    Page<ViolationAdminItem> getAdminViolationItems(
            Pageable pageable,
            List<Constants.ViolationTypeEnum> violationTypes,
            List<Constants.ViolationStatusEnum> violationStatusEnums,
            String name,
            Integer month,
            Integer year
    );

    Page<ViolationUserItem> getMyViolationItems(Pageable pageable);

    ViolationUserDetails getViolationUserDetailsById(UUID id);

    ViolationAdminDetails getViolationAdminDetailsById(UUID id);

    ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles);

    ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request);
}
