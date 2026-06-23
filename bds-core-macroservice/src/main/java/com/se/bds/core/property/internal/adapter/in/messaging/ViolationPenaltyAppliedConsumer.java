package com.se.bds.core.property.internal.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.ViolationPenaltyAppliedEvent;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.application.command.UpdatePropertyStatusCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViolationPenaltyAppliedConsumer {

    private final PropertyUseCase propertyUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "violation-penalty-applied", groupId = "core-macroservice-violation")
    public void consumeViolationPenaltyApplied(@Payload String payload) throws Exception {
        log.info("[KAFKA] Received violation-penalty-applied event, payload={}", payload);
        // Bubble deserialization or business execution exceptions up to trigger the Kafka CommonErrorHandler
        ViolationPenaltyAppliedEvent event = objectMapper.readValue(payload, ViolationPenaltyAppliedEvent.class);
        log.info("[KAFKA] Parsed violation-penalty-applied event: ID={}, reportedEntityId={}, reportedEntityType={}, penaltyApplied={}",
                event.violationId(), event.reportedEntityId(), event.reportedEntityType(), event.penaltyApplied());

        if ("PROPERTY".equalsIgnoreCase(event.reportedEntityType()) && "REMOVED_POST".equalsIgnoreCase(event.penaltyApplied())) {
            log.info("[KAFKA] Executing REMOVED_POST penalty for property ID={}", event.reportedEntityId());
            propertyUseCase.updatePropertyStatus(event.reportedEntityId(), new UpdatePropertyStatusCommand("REMOVED"));
            log.info("[KAFKA] Successfully set property ID={} status to REMOVED", event.reportedEntityId());
        } else {
            log.info("[KAFKA] Event did not match Core-owned property penalty. Skipping action.");
        }
    }
}
