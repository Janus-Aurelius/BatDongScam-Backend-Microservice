package com.se100.bds.models.entities.user;

import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.appointment.Appointment;
import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_agents")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaleAgent extends AbstractBaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "sale_agent_id", referencedColumnName = "user_id")
    private User user;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "max_properties", nullable = false)
    private int maxProperties;

    @Column(name = "hired_date", nullable = false)
    private LocalDateTime hiredDate;

    @OneToMany(mappedBy = "assignedAgent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Property> assignedProperties = new ArrayList<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Contract> contracts = new ArrayList<>();
}
