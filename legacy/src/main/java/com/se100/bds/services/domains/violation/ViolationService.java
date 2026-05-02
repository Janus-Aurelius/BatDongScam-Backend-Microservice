package com.se100.bds.services.domains.violation;

import com.se100.bds.dtos.requests.violation.UpdateViolationRequest;
import com.se100.bds.dtos.requests.violation.ViolationCreateRequest;
import com.se100.bds.dtos.responses.violation.*;
import com.se100.bds.utils.Constants;
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
            Integer month, Integer year
    );
    Page<ViolationUserItem> getMyViolationItems(Pageable pageable);
    ViolationUserDetails getViolationUserDetailsById(UUID id);
    ViolationAdminDetails getViolationAdminDetailsById(UUID id);

    ViolationUserDetails createViolationReport(ViolationCreateRequest request, MultipartFile[] evidenceFiles);
    ViolationAdminDetails updateViolationReport(UUID id, UpdateViolationRequest request);
}
