package com.se100.bds.services.payment.payway.exceptions;

import lombok.Getter;

/**
 * Represents a non-2xx response from Payway.
 * <p>
 * We keep the raw response body (if any) because Payway may return plain text or JSON
 * depending on the failure mode, and it's useful for troubleshooting.
 * </p>
 */
@Getter
public class PaywayApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public PaywayApiException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}

