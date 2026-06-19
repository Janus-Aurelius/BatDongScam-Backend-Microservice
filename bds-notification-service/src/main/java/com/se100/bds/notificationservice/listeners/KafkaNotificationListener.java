package com.se100.bds.notificationservice.listeners;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.NotificationTypeEnum;
import com.se.bds.common.enums.RelatedEntityTypeEnum;
import com.se.bds.common.event.ContractStatusChangedFatEvent;
import com.se100.bds.notificationservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REFACTORED: loại bỏ CoreServiceClient.
 *
 * handleContractStatusChanged:
 *   Trước: nhận contractId → gọi coreServiceClient.getContractById(id) → lấy customerId
 *   Sau:   đọc customerId trực tiếp từ ContractStatusChangedFatEvent payload
 *
 * Kết quả: 0 Feign call trong listener.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    // CoreServiceClient đã bị xóa

    /**
     * CHANGED: Parse ContractStatusChangedFatEvent - có sẵn customerId.
     * Không cần gọi coreServiceClient.getContractById() nữa.
     */
    @KafkaListener(topics = "contract-status-changed", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleContractStatusChanged(String message) {
        log.info("Received contract-status-changed event");
        try {
            ContractStatusChangedFatEvent event = objectMapper.readValue(message, ContractStatusChangedFatEvent.class);

            if (event.customerId() == null) {
                log.warn("ContractStatusChangedFatEvent missing customerId for contractId={}", event.contractId());
                return;
            }

            String title = "Contract Status Updated";
            String body = String.format("Your %s contract status is now %s.",
                    event.contractType() != null ? event.contractType().toLowerCase() : "rental",
                    event.newStatus() != null ? event.newStatus().toLowerCase() : "updated");

            notificationService.createNotification(
                    event.customerId(),
                    null,
                    NotificationTypeEnum.CONTRACT_UPDATE,
                    title,
                    body,
                    RelatedEntityTypeEnum.CONTRACT,
                    event.contractId().toString(),
                    null
            );
        } catch (Exception e) {
            log.error("Error processing contract-status-changed event", e);
            throw new RuntimeException("Failed to process contract-status-changed event", e);
        }
    }

    @KafkaListener(topics = "payment-completed", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentCompleted(String message) {
        log.info("Received payment-completed event");
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID paymentId = extractUuid(node, "paymentId");
            UUID payerUserId = node.has("payerUserId") && !node.get("payerUserId").isNull()
                    ? UUID.fromString(node.get("payerUserId").asText())
                    : (node.has("payerId") && !node.get("payerId").isNull()
                    ? UUID.fromString(node.get("payerId").asText()) : null);

            if (payerUserId != null && paymentId != null) {
                String body = "Your payment of amount "
                        + (node.has("amount") ? node.get("amount").asText() : "")
                        + " has been successfully processed.";
                notificationService.createNotification(
                        payerUserId, null,
                        NotificationTypeEnum.SYSTEM_ALERT,
                        "Payment Completed", body,
                        RelatedEntityTypeEnum.PAYMENT,
                        paymentId.toString(), null
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment-completed event", e);
            throw new RuntimeException("Failed to process payment-completed event", e);
        }
    }

    @KafkaListener(topics = "payment-due", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentDue(String message) {
        log.info("Received payment-due event");
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID recipientUserId = extractUuid(node, "recipientUserId");
            UUID paymentId = extractUuid(node, "paymentId");
            if (recipientUserId != null && paymentId != null) {
                String body = "You have a payment due of amount "
                        + (node.has("amount") ? node.get("amount").asText() : "")
                        + " on " + (node.has("dueDate") ? node.get("dueDate").asText() : "") + ".";
                notificationService.createNotification(
                        recipientUserId, null,
                        NotificationTypeEnum.PAYMENT_DUE,
                        "Payment Due Reminder", body,
                        RelatedEntityTypeEnum.PAYMENT,
                        paymentId.toString(), null
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment-due event", e);
            throw new RuntimeException("Failed to process payment-due event", e);
        }
    }

    @KafkaListener(topics = "payment-overdue", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentOverdue(String message) {
        log.info("Received payment-overdue event");
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID recipientUserId = extractUuid(node, "recipientUserId");
            UUID paymentId = extractUuid(node, "paymentId");
            if (recipientUserId != null && paymentId != null) {
                String body = "Your payment of amount "
                        + (node.has("amount") ? node.get("amount").asText() : "")
                        + " is overdue by "
                        + (node.has("daysOverdue") ? node.get("daysOverdue").asText() : "")
                        + " days. Please complete it immediately.";
                notificationService.createNotification(
                        recipientUserId, null,
                        NotificationTypeEnum.PAYMENT_OVERDUE,
                        "Payment Overdue Warning", body,
                        RelatedEntityTypeEnum.PAYMENT,
                        paymentId.toString(), null
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment-overdue event", e);
            throw new RuntimeException("Failed to process payment-overdue event", e);
        }
    }

    @KafkaListener(topics = "notification-requests", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleNotificationRequest(String message) {
        log.info("Received notification-requests event");
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID recipientUserId = extractUuid(node, "recipientUserId");
            String title = node.has("title") ? node.get("title").asText() : "";
            String body = node.has("body") ? node.get("body").asText() : "";
            String notificationType = node.has("notificationType") ? node.get("notificationType").asText() : "SYSTEM_ALERT";
            UUID relatedEntityId = node.has("relatedEntityId") && !node.get("relatedEntityId").isNull()
                    ? UUID.fromString(node.get("relatedEntityId").asText()) : null;
            String relatedEntityType = node.has("relatedEntityType") && !node.get("relatedEntityType").isNull()
                    ? node.get("relatedEntityType").asText() : null;

            if (recipientUserId == null) return;

            NotificationTypeEnum typeEnum = NotificationTypeEnum.SYSTEM_ALERT;
            try {
                typeEnum = NotificationTypeEnum.get(notificationType);
            } catch (Exception e) {
                log.warn("Invalid notificationType: {}. Defaulting to SYSTEM_ALERT.", notificationType);
            }

            RelatedEntityTypeEnum entityTypeEnum = null;
            if (relatedEntityType != null) {
                try {
                    entityTypeEnum = RelatedEntityTypeEnum.get(relatedEntityType);
                } catch (Exception e) {
                    log.warn("Invalid relatedEntityType: {}", relatedEntityType);
                }
            }

            notificationService.createNotification(
                    recipientUserId, null, typeEnum, title, body,
                    entityTypeEnum,
                    relatedEntityId != null ? relatedEntityId.toString() : null,
                    null
            );
        } catch (Exception e) {
            log.error("Error processing notification-requests event", e);
            throw new RuntimeException("Failed to process notification-requests event", e);
        }
    }

    // ---- Utility ----

    private UUID extractUuid(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        JsonNode fieldNode = node.get(field);
        if (fieldNode.isObject() && fieldNode.has("value")) {
            return UUID.fromString(fieldNode.get("value").asText());
        }
        return UUID.fromString(fieldNode.asText());
    }
}