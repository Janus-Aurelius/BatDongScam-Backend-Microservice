package com.se100.bds.models.entities.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.models.entities.document.IdentificationDocument;
import com.se100.bds.models.entities.violation.ViolationReport;
import com.se100.bds.models.entities.appointment.Appointment;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "property_id", nullable = false)),
})
public class Property extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private PropertyOwner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    @JsonIgnore
    private SaleAgent assignedAgent;

    @Column(name = "service_fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFeeAmount;

    @Column(name = "service_fee_collected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFeeCollectedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id", nullable = false)
    @JsonIgnore
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    @JsonIgnore
    private Ward ward;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private Constants.TransactionTypeEnum transactionType;

    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "area", nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "rooms")
    private Integer rooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "floors")
    private Integer floors;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "house_orientation", length = 100)
    private Constants.OrientationEnum houseOrientation;

    @Enumerated(EnumType.STRING)
    @Column(name = "balcony_orientation", length = 100)
    private Constants.OrientationEnum balconyOrientation;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_per_square_meter", precision = 15, scale = 2)
    private BigDecimal pricePerSquareMeter;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Constants.PropertyStatusEnum status;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Media> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Contract> contracts = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<IdentificationDocument> documents = new ArrayList<>();
}
