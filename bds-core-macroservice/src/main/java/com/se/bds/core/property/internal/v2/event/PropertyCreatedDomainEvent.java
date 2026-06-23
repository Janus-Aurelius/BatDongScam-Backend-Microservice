package com.se.bds.core.property.internal.v2.event;

import com.se.bds.core.property.internal.domain.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event representing the creation of a property listing.
 */
public record PropertyCreatedDomainEvent(
    UUID propertyId,
    UUID ownerId,
    UUID wardId,
    String title,
    String description,
    BigDecimal priceAmount,
    TransactionType transactionType,
    LocalDateTime occurredAt
) {}
