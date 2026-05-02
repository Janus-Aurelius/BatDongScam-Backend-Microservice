package com.se100.bds.services.payment.payway.exceptions;

/** 5xx from Payway. */
public class PaywayServerException extends PaywayApiException {
    public PaywayServerException(int statusCode, String message, String responseBody) {
        super(statusCode, message, responseBody);
    }
}

