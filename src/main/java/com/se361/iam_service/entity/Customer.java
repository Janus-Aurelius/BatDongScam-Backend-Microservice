package com.se361.iam_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends AbstractBaseEntity{

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id", referencedColumnName = "user_id")
    private User user;

}
