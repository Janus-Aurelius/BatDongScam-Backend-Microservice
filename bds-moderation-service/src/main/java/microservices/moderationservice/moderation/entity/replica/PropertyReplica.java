package microservices.moderationservice.moderation.entity.replica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_replicas")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyReplica {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "title")
    private String title;

    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price_amount", precision = 38, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
