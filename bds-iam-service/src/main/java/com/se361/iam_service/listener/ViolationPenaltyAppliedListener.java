package com.se361.iam_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.ViolationPenaltyAppliedEvent;
import com.se361.iam_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViolationPenaltyAppliedListener {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    @KafkaListener(topics = "violation-penalty-applied", groupId = "iam-service-violation")
    public void onViolationPenaltyApplied(String payload) {
        log.info("[KAFKA] IAM received violation-penalty-applied event: {}", payload);
        try {
            ViolationPenaltyAppliedEvent event = objectMapper.readValue(payload, ViolationPenaltyAppliedEvent.class);
            if (!"SUSPENDED_ACCOUNT".equalsIgnoreCase(event.penaltyApplied())) {
                log.info("[KAFKA] Penalty {} is not IAM-owned. Skipping.", event.penaltyApplied());
                return;
            }
            if (event.reportedUserId() == null) {
                log.warn("[KAFKA] reportedUserId is null in SUSPENDED_ACCOUNT event. Skipping.");
                return;
            }
            userService.updateStatus(event.reportedUserId(), "SUSPENDED");
            log.info("[KAFKA] Suspended user ID={} from violation penalty event={}",
                    event.reportedUserId(), event.violationId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to process violation-penalty-applied event in IAM", e);
        }
    }
}
