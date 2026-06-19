package com.se.bds.core.transaction.internal.adapter.in.messaging.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.event.SagaPaymentResult;
import com.se.bds.core.transaction.internal.application.service.v2.ContractPaymentSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes responses and result events emitted by the financial service back to the core macroservice.
 * Forwards payload processing to the central Saga Orchestrator.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SagaResponseListener {

    private final ContractPaymentSagaOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    /**
     * Listens to the contract saga events queue.
     * Routes payment outcomes or compensation confirmations to the orchestrator.
     */
    @KafkaListener(topics = "contract-saga-events", groupId = "core-macroservice-saga")
    public void consumeSagaEvent(@Payload String payload) {
        log.info("[SagaResponseListener] Consuming saga event: {}", payload);
        try {
            SagaPaymentResult result = objectMapper.readValue(payload, SagaPaymentResult.class);
            
            if ("REFUNDED".equalsIgnoreCase(result.status())) {
                orchestrator.handleRefundResult(result);
            } else {
                orchestrator.handlePaymentResult(result);
            }
            log.info("[SagaResponseListener] Saga event dispatched to orchestrator successfully");
        } catch (Exception e) {
            log.error("[SagaResponseListener] Failed to decode or handle saga event response", e);
        }
    }
}
