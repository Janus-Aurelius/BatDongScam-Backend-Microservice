package com.se100.bds.models.entities.contract;

import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "contract_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "contract_id", nullable = false)),
})
public abstract class Contract extends AbstractBaseEntity {
    // main contract parties

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private SaleAgent agent;

    // contract details

    @Column(name = "contract_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Constants.ContractTypeEnum contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Constants.ContractStatusEnum status;

    /// Contract number (or ID) of the actual, physical contract document
    @Column(name = "contract_number", unique = true, length = 50)
    private String contractNumber;

    /// Date when the contract terms becomes effective
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /// Date when the contract terms ends
    @Column(name = "end_date")
    private LocalDate endDate;

    /// Date that the PHYSICAL contract is legalized and signed by all parties
    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "special_terms", columnDefinition = "TEXT")
    private String specialTerms;

    // Cancellation specific fields

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /// For deposit contracts, if null, this will use the deposit amount
    @Column(name = "cancellation_penalty", precision = 15, scale = 2)
    private BigDecimal cancellationPenalty;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by")
    private Constants.RoleEnum cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Payments relationship

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    // Rating and review

    @Column(name = "rating")
    private Short rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
