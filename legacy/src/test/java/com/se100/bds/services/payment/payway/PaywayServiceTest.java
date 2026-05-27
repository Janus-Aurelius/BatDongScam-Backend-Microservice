package com.se100.bds.services.payment.payway;

import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.services.payment.dto.CreatePayoutSessionRequest;
import com.se100.bds.services.payment.dto.CreatePayoutSessionResponse;
import com.se100.bds.services.payment.dto.PayoutSessionResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

//! Note: This is a temporary smoke test for local development only.
//! It is not intended to be a full integration test or unit test.
//! It always passes, printing results to console.
//! Once we integrated the service fully, we can remove this class.
class PaywayServiceTest {

    private static PaywayService newPaywayServiceForLocalSmokeTest() {
        PaywayService service = new PaywayService();

        // NOTE: We intentionally do not boot Spring here.
        // We just inject @Value fields directly so application.yaml placeholders / .env aren't needed.
        setField(service, "serviceUrl", System.getProperty("payway.service-url", "http://localhost:3000"));
        setField(service, "apiKey", System.getProperty("payway.api-key", "my-client-id"));
        setField(service, "webhookBaseUrl", System.getProperty("payway.webhook-base-url", "http://localhost:8081/webhooks"));
        setField(service, "returnUrl", System.getProperty("payway.return-url", "http://localhost:5000"));

        return service;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to set field " + fieldName + " on " + target.getClass().getSimpleName(), e);
        }
    }

    @Test
    void smokeTest_createAndGetPaymentSession_printToConsole_alwaysPasses() {
        PaywayService service = newPaywayServiceForLocalSmokeTest();

        try {
            String idempotencyKey = "test-idempotency-payment-001";

            CreatePaymentSessionRequest req = CreatePaymentSessionRequest.builder()
                    .amount(new BigDecimal(1999))
                    .currency("USD")
                    .description("Payway smoke test")
                    .metadata(Map.of("source", "PaywayServiceTest"))
                    .build();

            CreatePaymentSessionResponse created = service.createPaymentSession(req, idempotencyKey);
            System.out.println("[PaywayServiceTest] createPaymentSession => " + created);

            if (created != null && created.getId() != null) {
                CreatePaymentSessionResponse fetched = service.getPaymentSession(created.getId());
                System.out.println("[PaywayServiceTest] getPaymentSession(" + created.getId() + ") => " + fetched);
            }

        } catch (Exception e) {
            // Temporary smoke test: do not fail build.
            System.out.println("[PaywayServiceTest] Payment smoke test error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Test
    void smokeTest_createAndGetPayoutSession_printToConsole_alwaysPasses() {
        PaywayService service = newPaywayServiceForLocalSmokeTest();

        try {
            String idempotencyKey = "test-idempotency-payout-001";

            CreatePayoutSessionRequest req = CreatePayoutSessionRequest.builder()
                    .amount(new BigDecimal(1234))
                    .currency("USD")
                    .accountNumber("1234567890")
                    .accountHolderName("Jane Doe")
                    .swiftCode("FOOBARXX")
                    .description("Payway payout smoke test")
                    .metadata(Map.of("source", "PaywayServiceTest"))
                    .build();

            CreatePayoutSessionResponse created = service.createPayoutSession(req, idempotencyKey);
            System.out.println("[PaywayServiceTest] createPayoutSession => " + created);

            if (created != null && created.getId() != null) {
                PayoutSessionResponse fetched = service.getPayoutSession(created.getId());
                System.out.println("[PaywayServiceTest] getPayoutSession(" + created.getId() + ") => " + fetched);
            }

        } catch (Exception e) {
            // Temporary smoke test: do not fail build.
            System.out.println("[PaywayServiceTest] Payout smoke test error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
