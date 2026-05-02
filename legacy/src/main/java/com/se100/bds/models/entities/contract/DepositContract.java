package com.se100.bds.models.entities.contract;

import com.se100.bds.utils.Constants.MainContractTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "deposit_contract")
@DiscriminatorValue("DEPOSIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositContract extends Contract {

    /// The type of main contract this deposit is for (RENTAL or PURCHASE)
    @Enumerated(EnumType.STRING)
    @Column(name = "main_contract_type", nullable = false)
    private MainContractTypeEnum mainContractType;

    /// The deposit amount paid by the customer
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    /// The agreed upon price for the main contract.
    /// For rental: the monthly rent amount.
    /// For purchase: the property value.
    @Column(name = "agreed_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal agreedPrice;

    // Relationship to the main contract (rental or purchase)
    // This will be set when the main contract is created from this deposit

    @OneToOne(mappedBy = "depositContract", fetch = FetchType.LAZY)
    private RentalContract rentalContract;

    @OneToOne(mappedBy = "depositContract", fetch = FetchType.LAZY)
    private PurchaseContract purchaseContract;

    public boolean isLinkedToMainContract() {
        return (rentalContract != null) || (purchaseContract != null);
    }
}
