package microservices.appointmentservice.dtos.responses.notification;

import com.se.bds.common.dto.AbstractBaseDataResponse;
import microservices.appointmentservice.utils.Constants;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem extends AbstractBaseDataResponse {
    private Constants.NotificationTypeEnum type;
    private String title;
    private boolean isRead;
}
