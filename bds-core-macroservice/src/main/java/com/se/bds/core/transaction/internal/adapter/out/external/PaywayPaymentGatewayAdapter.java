package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.core.transaction.internal.application.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PaywayPaymentGatewayAdapter implements PaymentGatewayPort {

    private final String apiKey;
    private final String merchantId;

    public PaywayPaymentGatewayAdapter(
            @Value("${payway.api-key:default-key}") String apiKey,
            @Value("${payway.merchant-id:default-merchant}") String merchantId) {
        this.apiKey = apiKey;
        this.merchantId = merchantId;
    }

    @Override
    public PaymentSessionResult createPaymentSession(
            BigDecimal amount,
            String currency,
            String description,
            Map<String, Object> metadata,
            String idempotencyKey) {
        
        log.info("[EVENT] Creating Payway payment session: amount={}, currency={}, description={}", amount, currency, description);
        
        // TODO: integrate with Payway sandbox for end-to-end payment flow
        
        String gatewayPaymentId = "payway_" + UUID.randomUUID().toString().substring(0, 8);
        String mockCheckoutUrl = "https://checkout.payway.com.vn/pay?session_id=" + gatewayPaymentId;

        log.info("[EVENT] Payment session created: paymentId={}, checkoutUrl={}", gatewayPaymentId, mockCheckoutUrl);

        return new PaymentSessionResult(
                gatewayPaymentId,
                mockCheckoutUrl,
                "PENDING",
                amount,
                currency
        );
    }

    @Override
    public PaymentSessionResult getPaymentSession(String gatewayPaymentId) {
        log.info("[EVENT] Fetching Payway payment session: {}", gatewayPaymentId);
        
        // TODO: integrate with Payway sandbox for end-to-end payment flow
        
        return new PaymentSessionResult(
                gatewayPaymentId,
                "https://checkout.payway.com.vn/pay?session_id=" + gatewayPaymentId,
                "SUCCESS",
                BigDecimal.ZERO,
                "VND"
        );
    }

    @Override
    public PayoutResult createPayoutSession(
            BigDecimal amount,
            String currency,
            String accountNumber,
            String accountHolderName,
            String swiftCode,
            String description,
            Map<String, Object> metadata,
            String idempotencyKey) {
        
        log.info("[EVENT] Triggering Payway payout: amount={}, bankAccount={}, holder={}", amount, accountNumber, accountHolderName);
        
        // TODO: integrate with Payway sandbox for end-to-end payment flow
        
        String gatewayPayoutId = "payout_" + UUID.randomUUID().toString().substring(0, 8);
        return new PayoutResult(
                gatewayPayoutId,
                "SUCCESS",
                amount,
                currency
        );
    }
}
