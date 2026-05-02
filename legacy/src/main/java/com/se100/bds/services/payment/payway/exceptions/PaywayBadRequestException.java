package com.se100.bds.services.payment.payway.exceptions;

/** 400 from Payway. */
public class PaywayBadRequestException extends PaywayApiException {
    public PaywayBadRequestException(String message, String responseBody) {
        super(400, message, responseBody);
    }
}
