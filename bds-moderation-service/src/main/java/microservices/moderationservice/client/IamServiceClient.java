package microservices.moderationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;
import java.util.Map;

@FeignClient(name = "iam-service", contextId = "iamServiceClient")
public interface IamServiceClient {
    @GetMapping("/users/validate")
    Map<String, Object> validateUser(@RequestParam("userId") UUID userId, @RequestParam("role") String role);

    @GetMapping("/api/account/{userId}")
    Map<String, Object> getUserDetails(@PathVariable("userId") UUID userId);
}
