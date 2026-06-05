package microservices.appointmentservice.entities.contract;

import microservices.appointmentservice.entities.AbstractBaseEntity;
import microservices.appointmentservice.entities.property.Property;
import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "payment_id", nullable = false)),
})
public class Payment extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_user_id")
    private User payer;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private Constants.PaymentTypeEnum paymentType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_time")
    private LocalDateTime paidTime;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "status")
    private Constants.PaymentStatusEnum status;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "stripe_session_id", unique = true, length = 255)
    private String stripeSessionId;
}