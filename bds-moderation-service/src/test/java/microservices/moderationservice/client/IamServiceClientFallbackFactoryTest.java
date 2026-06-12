package microservices.moderationservice.client;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IamServiceClientFallbackFactoryTest {

    private IamServiceClientFallbackFactory fallbackFactory;
    private IamServiceClient fallbackClient;

    @BeforeEach
    void setUp() {
        fallbackFactory = new IamServiceClientFallbackFactory();
    }

    @Test
    void validateUser_http500_returnsFailSecureResponse() {
        fallbackClient = fallbackFactory.create(http500Exception());

        Map<String, Object> result = fallbackClient.validateUser(UUID.randomUUID(), "CUSTOMER");

        assertEquals(false, result.get("active"));
        assertEquals("OFFLINE_FALLBACK", result.get("status"));
        assertEquals(true, result.get("fallback"));
    }

    @Test
    void validateUser_timeout_returnsFailSecureResponse() {
        fallbackClient = fallbackFactory.create(timeoutException());

        Map<String, Object> result = fallbackClient.validateUser(UUID.randomUUID(), "CUSTOMER");

        assertEquals(false, result.get("active"));
        assertEquals("OFFLINE_FALLBACK", result.get("status"));
    }

    @Test
    void getUserDetails_http500_returnsMockProfileData() {
        fallbackClient = fallbackFactory.create(http500Exception());

        Map<String, Object> result = fallbackClient.getUserDetails(UUID.randomUUID());

        assertEquals(true, result.get("success"));
        assertTrue(result.get("data") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("Profile Offline", data.get("fullName"));
        assertEquals("/assets/fallback-avatar.png", data.get("avatarUrl"));
        assertEquals("UNKNOWN", data.get("role"));
    }

    @Test
    void getUserDetails_timeout_returnsMockProfileData() {
        fallbackClient = fallbackFactory.create(timeoutException());

        Map<String, Object> result = fallbackClient.getUserDetails(UUID.randomUUID());

        assertEquals(true, result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("Profile Offline", data.get("fullName"));
    }

    private FeignException.InternalServerError http500Exception() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://iam-service/users/validate",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return new FeignException.InternalServerError("Internal Server Error", request, null, null);
    }

    private Throwable timeoutException() {
        return new SocketTimeoutException("Read timed out");
    }
}
