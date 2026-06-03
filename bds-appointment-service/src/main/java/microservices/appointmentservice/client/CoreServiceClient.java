package microservices.appointmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "core-macroservice", fallbackFactory = CoreServiceClientFallbackFactory.class)
public interface CoreServiceClient {

    @GetMapping("/public/properties/{propertyId}")
    Map<String, Object> getPropertyDetails(@PathVariable("propertyId") UUID propertyId);
}
