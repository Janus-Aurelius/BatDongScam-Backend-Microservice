package com.se100.bds.models.entities.user;

import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.property.Property;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property_owners")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "owner_id", nullable = false)),
})
public class PropertyOwner extends AbstractBaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private User user;

    @Column(name = "approved_at", nullable = true)
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Property> properties = new ArrayList<>();
}
