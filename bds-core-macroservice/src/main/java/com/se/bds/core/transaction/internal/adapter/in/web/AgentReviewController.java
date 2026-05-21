package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.application.port.in.AgentReviewUseCase;
import com.se.bds.core.transaction.internal.domain.model.AgentReview;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews/agent")
@RequiredArgsConstructor
public class AgentReviewController {

    private final AgentReviewUseCase agentReviewUseCase;

    public record SubmitReviewRequest(
            @NotNull UUID agentId,
            @NotNull UUID customerId,
            @NotNull UUID contractId,
            @Min(1) @Max(5) short rating,
            String comment
    ) {}

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<AgentReview> submitReview(@Valid @RequestBody SubmitReviewRequest request) {
        AgentReview review = agentReviewUseCase.submitReview(
                request.agentId(),
                request.customerId(),
                request.contractId(),
                request.rating(),
                request.comment()
        );
        return ResponseEntity.ok(review);
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<List<AgentReview>> getReviewsForAgent(@PathVariable("agentId") UUID agentId) {
        List<AgentReview> reviews = agentReviewUseCase.getReviewsForAgent(agentId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{agentId}/summary")
    public ResponseEntity<AgentReviewUseCase.AgentReviewSummary> getAgentReviewSummary(@PathVariable("agentId") UUID agentId) {
        AgentReviewUseCase.AgentReviewSummary summary = agentReviewUseCase.getAgentReviewSummary(agentId);
        return ResponseEntity.ok(summary);
    }
}
