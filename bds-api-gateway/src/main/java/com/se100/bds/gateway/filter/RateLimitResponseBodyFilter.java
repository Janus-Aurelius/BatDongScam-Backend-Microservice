package com.se100.bds.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that decorates the server response to intercept HTTP 429 (Too Many Requests).
 * When a 429 is detected (e.g. from the RequestRateLimiter gateway filter), this filter replaces
 * the empty or plain body with a structured, standardized JSON payload using {@link ApiResponse}.
 */
@Component
@Slf4j
public class RateLimitResponseBodyFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (HttpStatus.TOO_MANY_REQUESTS.equals(getStatusCode())) {
                    return writeCustomJsonBody();
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> setComplete() {
                if (HttpStatus.TOO_MANY_REQUESTS.equals(getStatusCode())) {
                    return writeCustomJsonBody();
                }
                return super.setComplete();
            }

            private Mono<Void> writeCustomJsonBody() {
                // If headers have already been sent, do nothing
                if (originalResponse.isCommitted()) {
                    return Mono.empty();
                }

                // Set headers to application/json
                originalResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                ApiResponse<Void> apiResponse = ApiResponse.error("Too Many Requests - Rate limit exceeded. Please try again later.");
                try {
                    byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
                    DataBuffer buffer = originalResponse.bufferFactory().wrap(bytes);
                    return originalResponse.writeWith(Mono.just(buffer));
                } catch (JsonProcessingException e) {
                    log.error("[RateLimiterDecorator] Failed to serialize error payload", e);
                    byte[] fallback = "{\"success\":false,\"message\":\"Too Many Requests - Rate limit exceeded. Please try again later.\",\"data\":null}".getBytes();
                    DataBuffer buffer = originalResponse.bufferFactory().wrap(fallback);
                    return originalResponse.writeWith(Mono.just(buffer));
                }
            }
        };

        // Pass the mutated exchange with our response decorator downstream
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // High priority. Must run before RequestRateLimiter filter (-100) to wrap response early.
        return -10000;
    }
}
