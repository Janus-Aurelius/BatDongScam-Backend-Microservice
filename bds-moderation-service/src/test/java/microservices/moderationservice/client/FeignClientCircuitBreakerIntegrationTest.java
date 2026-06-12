package microservices.moderationservice.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = FeignClientCircuitBreakerIntegrationTest.TestApplication.class,
        properties = {
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.openfeign.circuitbreaker.enabled=true",
                "resilience4j.circuitbreaker.instances.iamServiceClient.slidingWindowSize=5",
                "resilience4j.circuitbreaker.instances.iamServiceClient.minimumNumberOfCalls=1",
                "resilience4j.circuitbreaker.instances.iamServiceClient.failureRateThreshold=50",
                "resilience4j.timelimiter.instances.iamServiceClient.timeoutDuration=1s",
                "resilience4j.circuitbreaker.instances.coreServiceClient.slidingWindowSize=5",
                "resilience4j.circuitbreaker.instances.coreServiceClient.minimumNumberOfCalls=1",
                "resilience4j.circuitbreaker.instances.coreServiceClient.failureRateThreshold=50",
                "resilience4j.timelimiter.instances.coreServiceClient.timeoutDuration=1s"
        }
)
class FeignClientCircuitBreakerIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private IamServiceClient iamServiceClient;

    @Autowired
    private CoreServiceClient coreServiceClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerFeignUrls(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.iamServiceClient.url", wireMockServer::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.coreServiceClient.url", wireMockServer::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.iamServiceClient.connectTimeout", () -> "500");
        registry.add("spring.cloud.openfeign.client.config.iamServiceClient.readTimeout", () -> "1000");
        registry.add("spring.cloud.openfeign.client.config.coreServiceClient.connectTimeout", () -> "500");
        registry.add("spring.cloud.openfeign.client.config.coreServiceClient.readTimeout", () -> "1000");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void iamServiceClient_http500_triggersFallback() {
        UUID userId = UUID.randomUUID();
        wireMockServer.stubFor(get(urlPathEqualTo("/users/validate"))
                .willReturn(aResponse().withStatus(500)));

        Map<String, Object> result = iamServiceClient.validateUser(userId, "CUSTOMER");

        assertEquals(false, result.get("active"));
        assertEquals("OFFLINE_FALLBACK", result.get("status"));
        assertEquals(true, result.get("fallback"));
    }

    @Test
    void iamServiceClient_timeout_triggersFallback() {
        UUID userId = UUID.randomUUID();
        wireMockServer.stubFor(get(urlPathEqualTo("/users/validate"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(3000)));

        Map<String, Object> result = iamServiceClient.validateUser(userId, "CUSTOMER");

        assertEquals(false, result.get("active"));
        assertEquals("OFFLINE_FALLBACK", result.get("status"));
    }

    @Test
    void coreServiceClient_http500_triggersFallbackOnValidation() {
        UUID propertyId = UUID.randomUUID();
        wireMockServer.stubFor(get(urlPathMatching("/api/internal/properties/.*/location-info"))
                .willReturn(aResponse().withStatus(500)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> coreServiceClient.getPropertyLocationInfo(propertyId)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void coreServiceClient_timeout_triggersFallbackOnPropertyDetails() {
        UUID propertyId = UUID.randomUUID();
        wireMockServer.stubFor(get(urlPathMatching("/public/properties/.*"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(3000)));

        Map<String, Object> result = coreServiceClient.getPropertyDetails(propertyId);

        assertEquals("Property Offline", result.get("title"));
        assertEquals(true, result.get("fallback"));
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            KafkaAutoConfiguration.class
    })
    @EnableFeignClients(basePackages = "microservices.moderationservice.client")
    @ComponentScan(basePackages = "microservices.moderationservice.client")
    static class TestApplication {
    }
}
