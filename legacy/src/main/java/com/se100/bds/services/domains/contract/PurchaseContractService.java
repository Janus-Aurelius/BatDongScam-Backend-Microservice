package com.se100.bds.services.domains.contract;

import com.se100.bds.dtos.requests.contract.CancelPurchaseContractRequest;
import com.se100.bds.dtos.requests.contract.CreatePurchaseContractRequest;
import com.se100.bds.dtos.requests.contract.UpdatePurchaseContractRequest;
import com.se100.bds.dtos.responses.contract.PurchaseContractDetailResponse;
import com.se100.bds.dtos.responses.contract.PurchaseContractListItem;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing purchase contracts.
 * Purchase contracts represent complete property purchases.
 */
public interface PurchaseContractService {

    // =====================
    // PURCHASE CONTRACT CRUD
    // =====================

    /**
     * Create a new purchase contract in DRAFT state.
     * Only agents and admins can create.
     * Only one non-DRAFT purchase contract per property is allowed.
     * If depositContractId is provided, validates deposit is ACTIVE, not expired, and propertyValue matches.
     */
    PurchaseContractDetailResponse createPurchaseContract(CreatePurchaseContractRequest request);

    /**
     * Get purchase contract by ID with all details.
     */
    PurchaseContractDetailResponse getPurchaseContractById(UUID contractId);

    /**
     * Query purchase contracts with filters.
     */
    Page<PurchaseContractListItem> getPurchaseContracts(
            Pageable pageable,
            List<ContractStatusEnum> statuses,
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            String search
    );

    /**
     * Update a DRAFT purchase contract.
     * Only agents (assigned) and admins can update.
     */
    PurchaseContractDetailResponse updatePurchaseContract(UUID contractId, UpdatePurchaseContractRequest request);

    /**
     * Delete a DRAFT purchase contract (hard delete).
     * Only agents (assigned) and admins can delete.
     */
    void deletePurchaseContract(UUID contractId);

    // ==============================
    // PURCHASE CONTRACT TRANSITIONS
    // ==============================

    /**
     * Approve a purchase contract (DRAFT -> WAITING_OFFICIAL).
     * If advancePaymentAmount > 0, auto-creates advance payment and notifies customer.
     */
    PurchaseContractDetailResponse approvePurchaseContract(UUID contractId);

    /**
     * Mark paperwork as complete.
     * If remaining amount > 0, creates final payment and transitions to PENDING_PAYMENT.
     * If remaining amount = 0, transitions directly to COMPLETED.
     */
    PurchaseContractDetailResponse markPurchasePaperworkComplete(UUID contractId);

    /**
     * Cancel purchase contract by customer or owner.
     * - Before any payment: nothing happens
     * - After advance payment: refund advance to customer
     * - After final payment: not allowed (use void instead)
     */
    PurchaseContractDetailResponse cancelPurchaseContract(UUID contractId, CancelPurchaseContractRequest request);

    /**
     * Void purchase contract (admin only).
     * Just sets to CANCELLED with no side effects.
     */
    PurchaseContractDetailResponse voidPurchaseContract(UUID contractId);

    /**
     * Called when advance payment is completed (from webhook).
     * Notifies agent to continue paperwork.
     */
    void onAdvancePaymentCompleted(UUID contractId);

    /**
     * Called when final payment is completed (from webhook).
     * Triggers payout to owner and completes linked deposit contract.
     */
    void onFinalPaymentCompleted(UUID contractId);
}

