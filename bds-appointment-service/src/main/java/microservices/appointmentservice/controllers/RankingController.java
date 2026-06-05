package microservices.appointmentservice.controllers;

import com.se.bds.common.dto.ApiResponse;
import microservices.appointmentservice.schemas.ranking.*;
import microservices.appointmentservice.services.ranking.RankingService;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rankings")
@Slf4j
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/current-tier")
    public ResponseEntity<ApiResponse<String>> getCurrentTier(
            @RequestParam UUID userId,
            @RequestParam Constants.RoleEnum role
    ) {
        String tier = rankingService.getCurrentTier(userId, role);
        return ResponseEntity.ok(ApiResponse.success(tier));
    }

    // Sales Agent endpoints
    @GetMapping("/agent/{agentId}/month")
    public ResponseEntity<ApiResponse<IndividualSalesAgentPerformanceMonth>> getSaleAgentMonth(
            @PathVariable UUID agentId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        IndividualSalesAgentPerformanceMonth performance = rankingService.getSaleAgentMonth(agentId, month, year);
        return ResponseEntity.ok(ApiResponse.success(performance));
    }

    @GetMapping("/agent/{agentId}/career")
    public ResponseEntity<ApiResponse<IndividualSalesAgentPerformanceCareer>> getSaleAgentCareer(
            @PathVariable UUID agentId
    ) {
        IndividualSalesAgentPerformanceCareer career = rankingService.getSaleAgentCareer(agentId);
        return ResponseEntity.ok(ApiResponse.success(career));
    }

    // Customer endpoints
    @GetMapping("/customer/{customerId}/month")
    public ResponseEntity<ApiResponse<IndividualCustomerPotentialMonth>> getCustomerMonth(
            @PathVariable UUID customerId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        IndividualCustomerPotentialMonth potential = rankingService.getCustomerMonth(customerId, month, year);
        return ResponseEntity.ok(ApiResponse.success(potential));
    }

    @GetMapping("/customer/{customerId}/all")
    public ResponseEntity<ApiResponse<IndividualCustomerPotentialAll>> getCustomerAll(
            @PathVariable UUID customerId
    ) {
        IndividualCustomerPotentialAll potential = rankingService.getCustomerAll(customerId);
        return ResponseEntity.ok(ApiResponse.success(potential));
    }

    // Property Owner endpoints
    @GetMapping("/property-owner/{propertyOwnerId}/month")
    public ResponseEntity<ApiResponse<IndividualPropertyOwnerContributionMonth>> getPropertyOwnerMonth(
            @PathVariable UUID propertyOwnerId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        IndividualPropertyOwnerContributionMonth contribution = rankingService.getPropertyOwnerMonth(propertyOwnerId, month, year);
        return ResponseEntity.ok(ApiResponse.success(contribution));
    }

    @GetMapping("/property-owner/{propertyOwnerId}/all")
    public ResponseEntity<ApiResponse<IndividualPropertyOwnerContributionAll>> getPropertyOwnerAll(
            @PathVariable UUID propertyOwnerId
    ) {
        IndividualPropertyOwnerContributionAll contribution = rankingService.getPropertyOwnerAll(propertyOwnerId);
        return ResponseEntity.ok(ApiResponse.success(contribution));
    }
}
