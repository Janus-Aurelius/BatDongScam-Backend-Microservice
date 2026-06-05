package com.se.bds.core.property.internal.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.ViolationPenaltyAppliedEvent;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.application.command.UpdatePropertyStatusCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViolationPenaltyAppliedConsumer {

    private final PropertyUseCase propertyUseCase;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${iam.service-url:http://localhost:8084}")
    private String iamServiceUrl;

    @KafkaListener(topics = "violation-penalty-applied", groupId = "core-macroservice-violation")
    public void consumeViolationPenaltyApplied(@Payload String payload) {
        log.info("[KAFKA] Received violation-penalty-applied event, payload={}", payload);
        try {
            ViolationPenaltyAppliedEvent event = objectMapper.readValue(payload, ViolationPenaltyAppliedEvent.class);
            log.info("[KAFKA] Parsed violation-penalty-applied event: ID={}, reportedEntityId={}, reportedEntityType={}, penaltyApplied={}",
                    event.violationId(), event.reportedEntityId(), event.reportedEntityType(), event.penaltyApplied());

            if ("PROPERTY".equalsIgnoreCase(event.reportedEntityType()) && "REMOVED_POST".equalsIgnoreCase(event.penaltyApplied())) {
                log.info("[KAFKA] Executing REMOVED_POST penalty for property ID={}", event.reportedEntityId());
                propertyUseCase.updatePropertyStatus(event.reportedEntityId(), new UpdatePropertyStatusCommand("REMOVED"));
                log.info("[KAFKA] Successfully set property ID={} status to REMOVED", event.reportedEntityId());
            } else if ("SUSPENDED_ACCOUNT".equalsIgnoreCase(event.penaltyApplied())) {
                log.info("[KAFKA] Executing SUSPENDED_ACCOUNT penalty for user ID={}", event.reportedUserId());
                if (event.reportedUserId() != null) {
                    String url = iamServiceUrl + "/users/" + event.reportedUserId() + "/status?status=SUSPENDED";
                    restTemplate.put(url, null);
                    log.info("[KAFKA] Successfully sent suspension request to IAM service for user ID={}", event.reportedUserId());
                } else {
                    log.warn("[KAFKA] reportedUserId is null in penalty event, cannot suspend user");
                }
            } else {
                log.info("[KAFKA] Event did not match REMOVED_POST or SUSPENDED_ACCOUNT penalty. Skipping action.");
            }
        } catch (Exception e) {
            log.error("[KAFKA] Failed to deserialize or process ViolationPenaltyAppliedEvent", e);
        }
    }
}
