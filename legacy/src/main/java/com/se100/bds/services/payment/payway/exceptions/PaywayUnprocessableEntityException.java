package com.se100.bds.services.payment.payway.exceptions;

/** 422 from Payway (validation errors, missing required fields). */
public class PaywayUnprocessableEntityException extends PaywayApiException {
    public PaywayUnprocessableEntityException(String message, String responseBody) {
        super(422, message, responseBody);
    }
}

