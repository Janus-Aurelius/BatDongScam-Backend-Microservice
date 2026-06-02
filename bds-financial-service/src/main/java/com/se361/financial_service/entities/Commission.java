package com.se361.financial_service.entities;

import com.se361.financial_service.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commission extends AbstractBaseEntity {
    // === REFERENCES ===

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "property_id")
    private UUID propertyId;

    @Column(name = "agent_id")
    private UUID agentId;

    // === SNAPSHOT DATA ===

    @Column(name = "agent_name")
    private String agentName;

    @Column(name = "property_title")
    private String propertyTitle;

    // === COMMISSION DATA ===

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "transaction_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "commission_date", nullable = false)
    private LocalDate commissionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Constants.CommissionStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
