package com.se100.bds.services.domains.report;

import java.math.BigDecimal;
import java.util.UUID;

public interface FinancialUpdateService {
    // Every time a transaction for a property happens, call this method to update the finance
    // This function will find the property hierarchy location/type then update the revenue statistic
    // Ex: Buy, Rent, Pay service fee
    void transaction(UUID propertyId, BigDecimal value, int month, int year);

    // TODO: Pay money back logic handling
    // void payback
}
