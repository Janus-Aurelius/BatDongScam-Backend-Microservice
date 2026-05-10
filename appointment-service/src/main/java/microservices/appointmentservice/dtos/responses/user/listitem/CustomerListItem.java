package microservices.appointmentservice.dtos.responses.user.listitem;

import microservices.appointmentservice.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CustomerListItem extends AbstractBaseDataResponse {
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Integer ranking;
    private Integer point;
    private String tier;
    private BigDecimal totalSpending;
    private Integer totalViewings;
    private Integer totalContracts;
    private String location;
}
