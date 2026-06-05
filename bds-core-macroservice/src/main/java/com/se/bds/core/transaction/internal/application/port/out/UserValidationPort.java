package com.se.bds.core.transaction.internal.application.port.out;

import java.util.UUID;

public interface UserValidationPort {
    void validateCustomer(UUID customerId);
    void validateAgent(UUID agentId);
}
