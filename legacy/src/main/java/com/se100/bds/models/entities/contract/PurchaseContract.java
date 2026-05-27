package com.se100.bds.models.entities.contract;

import com.se100.bds.utils.Constants.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_contract")
@DiscriminatorValue("PURCHASE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseContract extends Contract {

    /// Reference to the deposit contract that preceded this purchase contract
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_contract_id")
    private DepositContract depositContract;

    /// The property value / purchase price
    @Column(name = "property_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal propertyValue;

    /// Down payment / advance payment amount
    @Column(name = "advance_payment_amount", precision = 15, scale = 2)
    private BigDecimal advancePaymentAmount;

    /// Commission amount for this purchase contract
    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    /// Calculates the remaining amount to be paid based on property value minus successful payments
    public BigDecimal getRemainingAmount() {
        if (propertyValue == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPaid = getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatusEnum.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return propertyValue.subtract(totalPaid);
    }
}

