package com.se100.bds.services.payment.payway.exceptions;

/** 401/403 from Payway (bad API key). */
public class PaywayUnauthorizedException extends PaywayApiException {
    public PaywayUnauthorizedException(int statusCode, String message, String responseBody) {
        super(statusCode, message, responseBody);
    }
}

