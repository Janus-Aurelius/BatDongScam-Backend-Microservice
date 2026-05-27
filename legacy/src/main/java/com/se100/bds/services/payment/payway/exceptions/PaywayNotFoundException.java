package com.se100.bds.services.payment.payway.exceptions;

/** 404 from Payway. */
public class PaywayNotFoundException extends PaywayApiException {
    public PaywayNotFoundException(String message, String responseBody) {
        super(404, message, responseBody);
    }
}

