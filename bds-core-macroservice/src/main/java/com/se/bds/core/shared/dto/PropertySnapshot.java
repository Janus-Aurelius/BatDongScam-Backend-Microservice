package com.se.bds.core.shared.dto;

import com.se.bds.core.shared.ids.PropertyId;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A read-only projection of a Property.
 * The Transaction module uses this to calculate commissions, validate
 * availability, and populate contract details — without ever touching
 * the Property JPA entity.
 *
 * <p>Fields mirror the original {@code Property} entity from the Property module.
 */
public record PropertySnapshot(

        // ── Identity ───────────────────────────────────────────────────
        PropertyId id,

        // ── Cross-module actor references ──────────────────────────────
        UUID ownerId,
        UUID assignedAgentId,

        // ── Location ───────────────────────────────────────────────────
        UUID wardId,
        String fullAddress,

        // ── Classification ─────────────────────────────────────────────
        String transactionType,
        String propertyTypeName,
        String status,

        // ── Listing info ───────────────────────────────────────────────
        String title,
        String description,

        // ── Physical attributes ────────────────────────────────────────
        BigDecimal area,
        Integer rooms,
        Integer bathrooms,
        Integer floors,
        Integer bedrooms,
        String houseOrientation,
        String balconyOrientation,
        Integer yearBuilt,
        String amenities,

        // ── Financial ──────────────────────────────────────────────────
        MonetaryAmount price,
        BigDecimal pricePerSquareMeter,
        BigDecimal commissionRate,
        BigDecimal serviceFeeAmount,
        BigDecimal serviceFeeCollectedAmount
) {
}
