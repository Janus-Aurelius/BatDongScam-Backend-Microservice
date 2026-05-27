package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.application.port.in.PaymentInitializationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentInitializationController {

    private final PaymentInitializationUseCase paymentInitializationUseCase;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/initialize")
    public ResponseEntity<PaymentInitializationUseCase.PaymentInitResult> initializePayment(
            @RequestParam("contractId") UUID contractId,
            @RequestParam("paymentId") UUID paymentId) {
        
        PaymentInitializationUseCase.PaymentInitResult result = paymentInitializationUseCase.initializePayment(contractId, paymentId);
        return ResponseEntity.ok(result);
    }
}
