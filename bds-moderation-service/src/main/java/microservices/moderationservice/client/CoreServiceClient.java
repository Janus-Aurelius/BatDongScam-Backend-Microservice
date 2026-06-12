package microservices.moderationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
import java.util.Map;

@FeignClient(
        name = "core-macroservice",
        contextId = "coreServiceClient",
        fallbackFactory = CoreServiceClientFallbackFactory.class
)
public interface CoreServiceClient {
    @GetMapping("/public/properties/{propertyId}")
    Map<String, Object> getPropertyDetails(@PathVariable("propertyId") UUID propertyId);

    @GetMapping("/api/internal/properties/{propertyId}/location-info")
    Map<String, Object> getPropertyLocationInfo(@PathVariable("propertyId") UUID propertyId);
}
