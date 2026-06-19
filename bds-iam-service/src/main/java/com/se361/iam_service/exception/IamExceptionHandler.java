package com.se361.iam_service.exception;

import com.se.bds.common.dto.ApiResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice specific to IAM Service to handle rate-limiting exceptions.
 * Intercepts Resilience4j RequestNotPermitted exceptions and maps them to standard 429 JSON responses.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // Takes precedence over standard exceptions
public class IamExceptionHandler {

    /**
     * Intercepts RequestNotPermitted thrown when a Resilience4j rate limiter is breached.
     * Returns an HTTP 429 status code with standard ApiResponse content.
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRequestNotPermitted(RequestNotPermitted ex) {
        log.warn("[RateLimiter] Rate limit exceeded at microservice layer: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error("Too Many Requests - Rate limit exceeded. Please try again later.");
        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }
}
