package com.se100.bds.gateway;

import com.se100.bds.gateway.config.AppProperties;
import com.se100.bds.gateway.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiGatewayRoutingTest {

    private AppProperties appProperties;
    private Environment env;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        appProperties = mock(AppProperties.class);
        env = mock(Environment.class);
        
        when(appProperties.getPublicPaths()).thenReturn(List.of("/api/search/**", "/public/**"));
        when(env.getActiveProfiles()).thenReturn(new String[]{"local"});
        
        filter = new JwtAuthenticationFilter(appProperties, env);
    }

    @Test
    void filter_publicPath_skipsValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/search/properties").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        boolean[] chainCalled = {false};
        GatewayFilterChain chain = ex -> {
            chainCalled[0] = true;
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled[0]);
        assertNull(exchange.getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    void filter_protectedPath_missingAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/notifications").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_mockToken_localProfile_succeedsAndForwardsHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/notifications")
                .header("Authorization", "Bearer admin")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean[] chainCalled = {false};
        GatewayFilterChain chain = ex -> {
            chainCalled[0] = true;
            assertEquals("admin-id", ex.getRequest().getHeaders().getFirst("X-User-Id"));
            assertEquals("ADMIN", ex.getRequest().getHeaders().getFirst("X-User-Roles"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled[0]);
    }
}
