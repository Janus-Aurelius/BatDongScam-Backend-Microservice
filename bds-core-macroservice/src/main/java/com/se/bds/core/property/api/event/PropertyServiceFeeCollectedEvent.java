package com.se.bds.core.property.api.event;

import com.se.bds.core.shared.ids.PropertyId;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyServiceFeeCollectedEvent (
    PropertyId propertyId,
    BigDecimal amountCollected,
    BigDecimal totalCollected,
    boolean fullyPaid,
    Instant occuredAt
    ){}
