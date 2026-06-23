package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.core.BaseIntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Integration test suite verifying the Resilience4j Circuit Breaker implementation on UserValidationAdapter.
 * Uses Spring's MockRestServiceServer to control HTTP network responses from the IAM service.
 */
public class UserValidationCircuitBreakerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserValidationAdapter userValidationAdapter;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private MockRestServiceServer mockServer;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("iamUserValidation");
        circuitBreaker.reset(); // Reset the circuit state to CLOSED before each test run
    }

    @Test
    void testUserValidationCircuitBreakerWorkflow() {
        UUID customerId = UUID.randomUUID();

        // Check initial state
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState(), "Circuit must start in CLOSED state");

        // Proof 1: Verify circuit transitions from CLOSED to OPEN after 5 consecutive failures
        // Note: minimumNumberOfCalls is configured as 5 in application.yml
        for (int i = 0; i < 5; i++) {
            mockServer.expect(requestTo(containsString("/users/validate")))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)); // Downstream service returns 500 error
        }

        for (int i = 0; i < 5; i++) {
            try {
                userValidationAdapter.validateCustomer(customerId);
                fail("Expected validation failure due to mock 500 errors");
            } catch (BusinessException e) {
                // Initial failures trigger the fallback logic, which wraps HTTP server errors into MSG12 BusinessExceptions
                assertEquals(MSG12.CODE, e.getCode());
                assertTrue(e.getMessage().contains("Identity validation service is currently unavailable"));
            }
        }

        // Verify that the mock server received exactly 5 calls
        mockServer.verify();

        // Proof 2: Verify the circuit breaker state has transitioned to OPEN
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState(), "Circuit must trip to OPEN after 5 consecutive failures");

        // Reset the mock server to ensure no additional HTTP requests are handled/expected
        mockServer = MockRestServiceServer.createServer(restTemplate);

        // Proof 3: Verify fast-fail behavior. Calls should fail immediately without reaching the HTTP server
        try {
            userValidationAdapter.validateCustomer(customerId);
            fail("Expected immediate failure while the circuit is OPEN");
        } catch (BusinessException e) {
            assertEquals(MSG12.CODE, e.getCode());
            assertTrue(e.getMessage().contains("Identity validation service is currently unavailable"));
        }

        // Verify that the mock server received 0 calls since the circuit is open (fails fast in memory)
        assertThrows(AssertionError.class, () -> mockServer.verify(), "No REST requests should be sent when circuit is OPEN");
    }
}
