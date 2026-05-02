package com.se100.bds.services.domains.contract;

import com.se100.bds.dtos.requests.contract.CreateRentalContractRequest;
import com.se100.bds.dtos.requests.contract.SecurityDepositDecisionRequest;
import com.se100.bds.dtos.requests.contract.UpdateRentalContractRequest;
import com.se100.bds.dtos.responses.contract.RentalContractDetailResponse;
import com.se100.bds.dtos.responses.contract.RentalContractListItem;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing rental contracts.
 * Rental contracts represent time-limited property rentals/leases.
 */
public interface RentalContractService {

    // =====================
    // RENTAL CONTRACT CRUD
    // =====================

    /**
     * Create a new rental contract in DRAFT state.
     * Only agents and admins can create.
     * Only one non-DRAFT rental contract per property is allowed.
     * If depositContractId is provided, validates deposit is ACTIVE, not expired, and monthlyRentAmount matches agreedPrice.
     */
    RentalContractDetailResponse createRentalContract(CreateRentalContractRequest request);

    /**
     * Get rental contract by ID with all details.
     */
    RentalContractDetailResponse getRentalContractById(UUID contractId);

    /**
     * Query rental contracts with filters.
     */
    Page<RentalContractListItem> getRentalContracts(
            Pageable pageable,
            List<ContractStatusEnum> statuses,
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            String search
    );

    /**
     * Update a DRAFT rental contract.
     * Only agents (assigned) and admins can update.
     */
    RentalContractDetailResponse updateRentalContract(UUID contractId, UpdateRentalContractRequest request);

    /**
     * Delete a DRAFT rental contract (hard delete).
     * Only agents (assigned) and admins can delete.
     */
    void deleteRentalContract(UUID contractId);

    // ==============================
    // RENTAL CONTRACT TRANSITIONS
    // ==============================

    /**
     * Approve a rental contract (DRAFT -> WAITING_OFFICIAL).
     */
    RentalContractDetailResponse approveRentalContract(UUID contractId);

    /**
     * Create security deposit payment for the customer.
     * Can only be called when contract is in WAITING_OFFICIAL state.
     * Only if securityDepositAmount > 0.
     */
    RentalContractDetailResponse createSecurityDepositPayment(UUID contractId);

    /**
     * Mark paperwork as complete.
     * Security deposit must be paid first if amount > 0.
     * Creates first month rent payment.
     * Transitions to PENDING_PAYMENT, then to ACTIVE once first month is paid.
     * Completes linked deposit contract when ACTIVE.
     */
    RentalContractDetailResponse markRentalPaperworkComplete(UUID contractId);

    /**
     * Void rental contract (admin only).
     * Just sets to CANCELLED with no side effects.
     */
    RentalContractDetailResponse voidRentalContract(UUID contractId);

    /**
     * Admin decides what happens to the security deposit.
     * Only callable when contract is ACTIVE or COMPLETED.
     * Triggers payout to customer or owner based on decision.
     */
    RentalContractDetailResponse decideSecurityDeposit(UUID contractId, SecurityDepositDecisionRequest request);

    /**
     * Called when security deposit payment is completed (from webhook).
     * Updates security deposit status to HELD.
     */
    void onSecurityDepositPaymentCompleted(UUID contractId);

    /**
     * Called when first month rent payment is completed (from webhook).
     * Transitions contract from PENDING_PAYMENT to ACTIVE.
     * Completes linked deposit contract.
     */
    void onFirstMonthRentPaymentCompleted(UUID contractId);

    /**
     * Called when a monthly rent payment is completed (from webhook).
     * Triggers payout to owner (monthlyRent - commission).
     */
    void onMonthlyRentPaymentCompleted(UUID contractId, UUID paymentId);

    /**
     * Complete rental contract when rental period ends.
     * Transitions from ACTIVE to COMPLETED.
     * Sends notification to owner and customer about security deposit decision.
     * Called by scheduled job when endDate is reached.
     */
    RentalContractDetailResponse completeRentalContract(UUID contractId);
}
