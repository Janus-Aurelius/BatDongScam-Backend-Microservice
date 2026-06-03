package com.se361.financial_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout extends AbstractBaseEntity {

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    @Column(name = "swift_code")
    private String swiftCode;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, PAID, FAILED

    @Column(name = "gateway_payout_id", unique = true)
    private String gatewayPayoutId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
