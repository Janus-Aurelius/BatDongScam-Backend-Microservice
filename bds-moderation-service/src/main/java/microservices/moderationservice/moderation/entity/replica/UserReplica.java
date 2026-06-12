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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_replicas")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserReplica {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "role")
    private String role;

    @Column(name = "status")
    private String status;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
