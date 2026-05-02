package com.se100.bds.models.entities.contract;

import com.se100.bds.utils.Constants.SecurityDepositStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_contract")
@DiscriminatorValue("RENTAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalContract extends Contract {

    /// Reference to the deposit contract that preceded this rental contract
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_contract_id")
    private DepositContract depositContract;

    /// Number of months for the rental period
    @Column(name = "month_count", nullable = false)
    private Integer monthCount;

    /// Monthly rent amount
    @Column(name = "monthly_rent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyRentAmount;

    /// Commission amount for this rental contract (taken monthly before sending to owner)
    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    /// Security deposit amount (optional, can be zero)
    @Column(name = "security_deposit_amount", precision = 15, scale = 2)
    private BigDecimal securityDepositAmount;

    /// Status of the security deposit
    @Column(name = "security_deposit_status")
    @Enumerated(EnumType.STRING)
    private SecurityDepositStatusEnum securityDepositStatus;

    /// When admin made the decision about security deposit
    @Column(name = "security_deposit_decision_at")
    private LocalDateTime securityDepositDecisionAt;

    /// Optional reason for the security deposit decision
    @Column(name = "security_deposit_decision_reason", length = 500)
    private String securityDepositDecisionReason;

    /// Late payment penalty rate (e.g., 0.05 for 5% per month)
    @Column(name = "late_payment_penalty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal latePaymentPenaltyRate;

    /// Accumulated unpaid penalty fee amount
    @Column(name = "accumulated_unpaid_penalty", nullable = false, precision = 15, scale = 2)
    private BigDecimal accumulatedUnpaidPenalty = BigDecimal.ZERO;

    /// Number of months with unpaid fees (for taking action when 3+ months unpaid)
    @Column(name = "unpaid_months_count", nullable = false)
    private Integer unpaidMonthsCount = 0;
}

