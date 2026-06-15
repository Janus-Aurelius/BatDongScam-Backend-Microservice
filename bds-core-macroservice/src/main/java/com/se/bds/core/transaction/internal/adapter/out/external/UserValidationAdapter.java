package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.transaction.internal.application.port.out.UserValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserValidationAdapter implements UserValidationPort {

    private final RestTemplate restTemplate;

    @Value("${iam.service-url:http://localhost:8084}")
    private String iamServiceUrl;

    @Override
    public void validateCustomer(UUID customerId) {
        validateUser(customerId, "CUSTOMER");
    }

    @Override
    public void validateAgent(UUID agentId) {
        validateUser(agentId, "SALESAGENT");
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "iamUserValidation", fallbackMethod = "fallbackValidateUser")
    private void validateUser(UUID userId, String role) {
        if (userId == null) {
            return;
        }
        String url = iamServiceUrl + "/users/validate?userId={userId}&role={role}";
        Map<String, Object> uriVariables = Map.of(
                "userId", userId.toString(),
                "role", role
        );

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class, uriVariables);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Boolean active = (Boolean) response.getBody().get("active");
            if (active == null || !active) {
                throw new BusinessException(MSG12.CODE, "User is not active or role does not match");
            }
        } else {
            throw new BusinessException(MSG12.CODE, "User validation failed with status " + response.getStatusCode());
        }
    }

    // Fallback method executes when Circuit Breaker is open
    private void fallbackValidateUser(UUID userId, String role, Throwable throwable) {
        if (throwable instanceof BusinessException) {
            throw (BusinessException) throwable;
        }
        log.error("Resilience fallback triggered for user validation (userId={}, role={}). Reason: {}", 
                userId, role, throwable.getMessage());
        throw new BusinessException(MSG12.CODE, "Identity validation service is currently unavailable. Please try again later.");
    }
}
