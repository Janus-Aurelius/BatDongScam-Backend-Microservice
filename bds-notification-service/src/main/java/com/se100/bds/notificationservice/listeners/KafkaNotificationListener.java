package com.se100.bds.notificationservice.listeners;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.NotificationTypeEnum;
import com.se.bds.common.enums.RelatedEntityTypeEnum;
import com.se100.bds.notificationservice.client.CoreServiceClient;
import com.se100.bds.notificationservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationListener {

    private final NotificationService notificationService;
    private final CoreServiceClient coreServiceClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "contract-status-changed", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleContractStatusChanged(String message) {
        log.info("Received contract-status-changed event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            JsonNode contractIdNode = node.get("contractId");
            UUID contractId = null;
            if (contractIdNode != null) {
                if (contractIdNode.isObject() && contractIdNode.has("value")) {
                    contractId = UUID.fromString(contractIdNode.get("value").asText());
                } else {
                    contractId = UUID.fromString(contractIdNode.asText());
                }
            }
            String contractType = node.has("contractType") ? node.get("contractType").asText() : "";
            String newStatus = node.has("newStatus") ? node.get("newStatus").asText() : "";

            if (contractId != null) {
                Map<String, Object> contract = coreServiceClient.getContractById(contractId);
                if (contract != null && contract.containsKey("customerId") && contract.get("customerId") != null) {
                    UUID customerId = UUID.fromString(contract.get("customerId").toString());
                    String title = "Contract Status Updated";
                    String body = String.format("Your %s contract status is now %s.", contractType.toLowerCase(), newStatus.toLowerCase());
                    notificationService.createNotification(
                            customerId,
                            null,
                            NotificationTypeEnum.CONTRACT_UPDATE,
                            title,
                            body,
                            RelatedEntityTypeEnum.CONTRACT,
                            contractId.toString(),
                            null
                    );
                } else {
                    log.warn("Contract customer ID not found for contract {}", contractId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing contract-status-changed event", e);
        }
    }

    @KafkaListener(topics = "payment-completed", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentCompleted(String message) {
        log.info("Received payment-completed event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID paymentId = node.has("paymentId") ?
                    (node.get("paymentId").isObject() && node.get("paymentId").has("value") ? UUID.fromString(node.get("paymentId").get("value").asText()) : UUID.fromString(node.get("paymentId").asText())) : null;
            UUID payerUserId = null;
            if (node.has("payerUserId") && !node.get("payerUserId").isNull()) {
                payerUserId = UUID.fromString(node.get("payerUserId").asText());
            } else if (node.has("payerId") && !node.get("payerId").isNull()) {
                payerUserId = UUID.fromString(node.get("payerId").asText());
            }

            if (payerUserId != null && paymentId != null) {
                String title = "Payment Completed";
                String body = "Your payment of amount " + (node.has("amount") ? node.get("amount").asText() : "") + " has been successfully processed.";
                notificationService.createNotification(
                        payerUserId,
                        null,
                        NotificationTypeEnum.SYSTEM_ALERT,
                        title,
                        body,
                        RelatedEntityTypeEnum.PAYMENT,
                        paymentId.toString(),
                        null
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment-completed event", e);
        }
    }

    @KafkaListener(topics = "payment-due", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentDue(String message) {
        log.info("Received payment-due event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID recipientUserId = node.has("recipientUserId") ? UUID.fromString(node.get("recipientUserId").asText()) : null;
            UUID paymentId = node.has("paymentId") ? UUID.fromString(node.get("paymentId").asText()) : null;

            if (recipientUserId != null && paymentId != null) {
                String title = "Payment Due Reminder";
                String body = "You have a payment due of amount " + (node.has("amount") ? node.get("amount").asText() : "") + " on " + (node.has("dueDate") ? node.get("dueDate").asText() : "") + ".";
                notificationService.createNotification(
                        recipientUserId,
                        null,
                        NotificationTypeEnum.PAYMENT_DUE,
                        title,
                        body,
                        RelatedEntityTypeEnum.PAYMENT,
                        paymentId.toString(),
                        null
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment-due event", e);
        }
    }

    @KafkaListener(topics = "payment-overdue", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentOverdue(String message) {
        log.info("Received payment-overdue event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID recipientUserId = node.has("recipientUserId") ? UUID.fromString(node.get("recipientUserId").asText()) : null;
            UUID paymentId = node.has("paymentId") ? UUID.fromString(node.get("paymentId").asText()) : null;

            if (recipientUserId != null && paymentId != null) {
                String title = "Payment Overdue Warning";
                String body = "Your payment of amount " + (node.has("amount") ? node.get("amount").asText() : "") + " is overdue by " + (node.has("daysOverdue") ? node.get("daysOverdue").asText() : "") + " days. Please complete it immediately.";
                notificationService.createNotification(
                        recipientUserId,
                        null,
                        NotificationTypeEnum.PAYMENT_OVERDUE,
                        title,
                        body,
                        RelatedEntityTypeEnum.PAYMENT,
                        paymentId.toString(),
                        null
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment-overdue event", e);
        }
    }

    @KafkaListener(topics = "notification-requests", groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleNotificationRequest(String message) {
        log.info("Received notification-requests event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID recipientUserId = node.has("recipientUserId") ? UUID.fromString(node.get("recipientUserId").asText()) : null;
            String title = node.has("title") ? node.get("title").asText() : "";
            String body = node.has("body") ? node.get("body").asText() : "";
            String notificationType = node.has("notificationType") ? node.get("notificationType").asText() : "SYSTEM_ALERT";
            UUID relatedEntityId = node.has("relatedEntityId") && !node.get("relatedEntityId").isNull() ? UUID.fromString(node.get("relatedEntityId").asText()) : null;
            String relatedEntityType = node.has("relatedEntityType") && !node.get("relatedEntityType").isNull() ? node.get("relatedEntityType").asText() : null;

            if (recipientUserId != null) {
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
                        recipientUserId,
                        null,
                        typeEnum,
                        title,
                        body,
                        entityTypeEnum,
                        relatedEntityId != null ? relatedEntityId.toString() : null,
                        null
                );
            }
        } catch (Exception e) {
            log.error("Error processing notification-requests event", e);
        }
    }
}
