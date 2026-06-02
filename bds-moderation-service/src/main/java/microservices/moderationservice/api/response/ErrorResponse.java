package microservices.moderationservice.api.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ErrorResponse extends AbstractBaseResponse {
    private String error;
}
