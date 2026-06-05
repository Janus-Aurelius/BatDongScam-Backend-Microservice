package microservices.moderationservice.moderation.service;

import com.se.bds.common.enums.*;
import microservices.moderationservice.moderation.dto.request.UpdateViolationRequest;
import microservices.moderationservice.moderation.dto.request.ViolationCreateRequest;
import microservices.moderationservice.moderation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ViolationService {
    Page<ViolationAdminItem> getAdminViolationItems(
            Pageable pageable,
            List<ViolationTypeEnum> violationTypes,
            List<ViolationStatusEnum> violationStatusEnums,
            String name,
            Integer month,
            Integer year
    );

    Page<ViolationUserItem> getMyViolationItems(Pageable pageable);

    ViolationUserDetails getViolationUserDetailsById(UUID id);

    ViolationAdminDetails getViolationAdminDetailsById(UUID id);

    ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles);

    ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request);

    ViolationReportStats getViolationStats(int year);
}
