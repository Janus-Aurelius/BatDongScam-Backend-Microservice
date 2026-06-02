package com.se361.iam_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_owners")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "owner_id", nullable = false))
})
public class PropertyOwner extends AbstractBaseEntity{

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private User user;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

}
