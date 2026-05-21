package com.se.bds.core.transaction.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Agent review submitted by a customer after a completed contract (US-030).
 * Stored in a separate table to support multiple reviews per agent
 * (one per contract) and efficient aggregation of average ratings.
 *
 * <p>A customer may only submit one review per contract.
 */
@Entity
@Table(name = "agent_review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_agent_review_contract", columnNames = {"contract_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", nullable = false)
    private UUID id;

    /** The agent being reviewed (references User module) */
    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    /** The customer who submitted the review (references User module) */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** The contract that this review is for */
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    /** The contract type (for filtering) */
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false)
    private ContractType contractType;

    /** Rating from 1 to 5 */
    @Column(name = "rating", nullable = false)
    private Short rating;

    /** Optional review comment */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Domain Logic ──

    /**
     * Validates that the rating is within acceptable bounds (1-5).
     */
    public void validateRating() {
        if (this.rating == null || this.rating < 1 || this.rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5, got: " + this.rating);
        }
    }
}
