package com.se100.bds.controllers.external;

import com.se100.bds.dtos.requests.payment.UpdatePaymentStatusRequest;
import com.se100.bds.dtos.responses.payment.PaymentDetailResponse;
import com.se100.bds.dtos.responses.payment.PaymentListItem;
import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.services.domains.payment.PaymentService;
import com.se100.bds.services.payment.payway.PaywayWebhookSignatureVerifier;
import com.se100.bds.utils.Constants.PaymentStatusEnum;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaywayWebhookController.class)
@TestPropertySource(properties = {
        "payway.verify-key=secret"
})
class PaywayWebhookControllerTest {

    @Autowired
    MockMvc mvc;

    @TestConfiguration
    static class Config {
        @Bean
        PaymentService paymentService() {
            return new PaymentService() {
                @Override
                public Page<PaymentListItem> getPayments(
                        Pageable pageable,
                        List<PaymentTypeEnum> paymentTypes,
                        List<PaymentStatusEnum> statuses,
                        UUID payerId,
                        UUID payeeId,
                        UUID contractId,
                        UUID propertyId,
                        UUID agentId,
                        LocalDate dueDateFrom,
                        LocalDate dueDateTo,
                        LocalDate paidDateFrom,
                        LocalDate paidDateTo,
                        Boolean overdue
                ) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Page<PaymentListItem> getPaymentsOfProperty(Pageable pageable, @NotNull UUID propertyId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Page<PaymentListItem> getPaymentsByPayer(Pageable pageable, @NotNull UUID payerId,
                        List<PaymentStatusEnum> statuses) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Page<PaymentListItem> getPaymentsByPayee(Pageable pageable, @NotNull UUID payeeId,
                        List<PaymentStatusEnum> statuses) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public PaymentDetailResponse getPaymentById(UUID paymentId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public PaymentDetailResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public PaymentDetailResponse createServiceFeePayment(Property property) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void handlePaywayWebhook(String rawBody) {
                    // no-op for slice test
                }

                @Override
                public boolean isPaymentAccessibleByPropertyOwner(UUID paymentId, UUID ownerUserId) {
                    return false;
                }
            };
        }
    }

    @Test
    void shouldRejectWhenMissingSignature() throws Exception {
        mvc.perform(post("/webhooks/payway")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptWhenSignatureValid() throws Exception {
        String body = "{\"x\":1}";
        String sigHex = PaywayWebhookSignatureVerifier.hmacSha256Hex("secret", body.getBytes(StandardCharsets.UTF_8));

        mvc.perform(post("/webhooks/payway")
                        .header("X-Signature", "sha256=" + sigHex)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectWhenSignatureInvalid() throws Exception {
        mvc.perform(post("/webhooks/payway")
                        .header("X-Signature", "sha256=deadbeef")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
