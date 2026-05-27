package com.se100.bds.services.payment.payway;

// TODO: fix this test

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.se100.bds.models.entities.contract.Payment;
//import com.se100.bds.repositories.domains.contract.PaymentRepository;
//import com.se100.bds.utils.Constants;
//import org.junit.jupiter.api.Test;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class PaywayWebhookHandlerTest {
//
//    @Test
//    void shouldUpdatePaymentStatusOnSucceededEvent() {
//        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
//        PaymentRepository repo = mock(PaymentRepository.class);
//
//        Payment p = new Payment();
//        p.setStatus(Constants.PaymentStatusEnum.PENDING);
//        p.setPaywayPaymentId("7f52f1e4-7e0f-4a40-8b68-99e972f1d2a1");
//
//        when(repo.findByPaywayPaymentId(p.getPaywayPaymentId())).thenReturn(Optional.of(p));
//        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
//
//        PaywayWebhookHandler handler = new PaywayWebhookHandler(mapper, repo);
//
//        String body = "{" +
//                "\"id\":\"evt-1\"," +
//                "\"type\":\"payment.succeeded\"," +
//                "\"created\":1734650000," +
//                "\"data\":{\"object\":{\"id\":\"" + p.getPaywayPaymentId() + "\",\"amount\":1999,\"currency\":\"USD\",\"status\":\"succeeded\",\"metadata\":{},\"created_at\":\"2025-12-20T12:34:56.789Z\",\"updated_at\":\"2025-12-20T12:35:10.123Z\"}}" +
//                "}";
//
//        handler.handlePaymentEvent(body);
//
//        verify(repo).findByPaywayPaymentId(p.getPaywayPaymentId());
//        verify(repo).save(any(Payment.class));
//        assertEquals(Constants.PaymentStatusEnum.SUCCESS, p.getStatus());
//    }
//}
