package microservices.appointmentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class CoreServiceClientFallbackFactory implements FallbackFactory<CoreServiceClient> {

    @Override
    public CoreServiceClient create(Throwable cause) {
        return new CoreServiceClient() {
            @Override
            public Map<String, Object> getPropertyDetails(UUID propertyId) {
                log.error("Feign fallback triggered for getPropertyDetails with propertyId={}, error: {}", propertyId, cause.getMessage());
                return null;
            }
        };
    }
}
