package microservices.moderationservice.moderation.controller;

import com.se.bds.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.moderationservice.moderation.dto.response.ViolationReportStats;
import microservices.moderationservice.moderation.service.ViolationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/violations/admin/reports")
@RequiredArgsConstructor
@Slf4j
public class ViolationReportController {

    private final ViolationService violationService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{year}")
    public ResponseEntity<ApiResponse<ViolationReportStats>> getViolationReportStats(@PathVariable int year) {
        log.info("Request to get violation stats for year: {}", year);
        ViolationReportStats stats = violationService.getViolationStats(year);
        if (stats == null) {
            return ResponseEntity.ok(ApiResponse.error("No statistics found for year " + year));
        }
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
