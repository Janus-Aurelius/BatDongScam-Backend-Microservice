package microservices.appointmentservice.entities.user;

import microservices.appointmentservice.entities.AbstractBaseEntity;
import microservices.appointmentservice.entities.appointment.Appointment;
import microservices.appointmentservice.entities.contract.Contract;
import microservices.appointmentservice.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends AbstractBaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id", referencedColumnName = "user_id")
    private User user;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Contract> contracts = new ArrayList<>();
}
