package com.se.bds.core.transaction.internal.domain.model;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.shared.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A deposit contract that precedes either a Rental or Purchase contract.
 *
 * <p>Intra-module relationships to {@link RentalContract} and
 * {@link PurchaseContract} are preserved since they belong to the same module.
 */
@Entity
@Table(name = "deposit_contract")
@DiscriminatorValue("DEPOSIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositContract extends Contract {

    /** The type of main contract this deposit is for (RENTAL or PURCHASE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "main_contract_type", nullable = false)
    private MainContractType mainContractType;

    /** The deposit amount paid by the customer */
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    /**
     * The agreed upon price for the main contract.
     * For rental: the monthly rent amount.
     * For purchase: the property value.
     */
    @Column(name = "agreed_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal agreedPrice;

    // ── Intra-module relationships (same Transaction module) ───────────

    @OneToOne(mappedBy = "depositContract", fetch = FetchType.LAZY)
    private RentalContract rentalContract;

    @OneToOne(mappedBy = "depositContract", fetch = FetchType.LAZY)
    private PurchaseContract purchaseContract;

    // ── Domain logic ───────────────────────────────────────────────────

    public boolean isLinkedToMainContract() {
        return (rentalContract != null) || (purchaseContract != null);
    }

    /**
     * Overrides base cancel to auto-set cancellation penalty to the deposit amount
     * if no explicit penalty was set.
     */
    @Override
    public ContractStatus cancel (String reason, Role initiator)
    {
        ContractStatus old = super.cancel(reason,initiator);
        if (getCancellationPenalty() == null)
        {
            setCancellationPenalty(this.depositAmount);
        }
        return old;
    }


    /**
     * Overrides base complete. A deposit contract should only be completed
     * when it has been linked to a main contract (rental or purchase).
     */
    @Override
    public ContractStatus complete()
    {
        if (!isLinkedToMainContract())
        {
            throw new BusinessException(MSG12.CODE, MSG12.MESSAGE);
        }
        return super.complete();
    }
}
