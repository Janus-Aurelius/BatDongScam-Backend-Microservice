package microservices.moderationservice.client;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CoreServiceClientFallbackFactoryTest {

    private CoreServiceClientFallbackFactory fallbackFactory;
    private CoreServiceClient fallbackClient;

    @BeforeEach
    void setUp() {
        fallbackFactory = new CoreServiceClientFallbackFactory();
    }

    @Test
    void getPropertyLocationInfo_http500_throwsServiceUnavailable() {
        fallbackClient = fallbackFactory.create(http500Exception());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fallbackClient.getPropertyLocationInfo(UUID.randomUUID())
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void getPropertyLocationInfo_timeout_throwsServiceUnavailable() {
        fallbackClient = fallbackFactory.create(timeoutException());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fallbackClient.getPropertyLocationInfo(UUID.randomUUID())
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void getPropertyDetails_http500_returnsMockPropertyData() {
        fallbackClient = fallbackFactory.create(http500Exception());

        Map<String, Object> result = fallbackClient.getPropertyDetails(UUID.randomUUID());

        assertEquals("Property Offline", result.get("title"));
        assertEquals("", result.get("thumbnailUrl"));
        assertEquals(true, result.get("fallback"));
    }

    @Test
    void getPropertyDetails_timeout_returnsMockPropertyData() {
        fallbackClient = fallbackFactory.create(timeoutException());

        Map<String, Object> result = fallbackClient.getPropertyDetails(UUID.randomUUID());

        assertEquals("Property Offline", result.get("title"));
        assertEquals(true, result.get("fallback"));
    }

    private FeignException.InternalServerError http500Exception() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://core-macroservice/api/internal/properties/123/location-info",
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
