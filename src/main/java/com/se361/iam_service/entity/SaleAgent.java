package com.se361.iam_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sale_agents")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaleAgent extends AbstractBaseEntity{

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

}
