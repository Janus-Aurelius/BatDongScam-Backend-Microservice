package com.se100.bds.services.domains.contract;

import com.se100.bds.dtos.requests.contract.CancelDepositContractRequest;
import com.se100.bds.dtos.requests.contract.CreateDepositContractRequest;
import com.se100.bds.dtos.requests.contract.UpdateDepositContractRequest;
import com.se100.bds.dtos.responses.contract.DepositContractDetailResponse;
import com.se100.bds.dtos.responses.contract.DepositContractListItem;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing deposit contracts.
 * Deposit contracts are optional pre-contracts that guarantee the main contract (rental or purchase)
 * will be signed within a defined time period.
 */
public interface DepositContractService {

    // =====================
    // DEPOSIT CONTRACT CRUD
    // =====================

    /**
     * Create a new deposit contract in DRAFT state.
     * Only agents and admins can create.
     * Only one non-DRAFT deposit contract per property is allowed.
     */
    DepositContractDetailResponse createDepositContract(CreateDepositContractRequest request);

    /**
     * Get deposit contract by ID with all details.
     */
    DepositContractDetailResponse getDepositContractById(UUID contractId);

    /**
     * Query deposit contracts with filters.
     */
    Page<DepositContractListItem> getDepositContracts(
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
     * Update a DRAFT deposit contract.
     * Only agents (assigned) and admins can update.
     */
    DepositContractDetailResponse updateDepositContract(UUID contractId, UpdateDepositContractRequest request);

    /**
     * Delete a DRAFT deposit contract (hard delete).
     * Only agents (assigned) and admins can delete.
     */
    void deleteDepositContract(UUID contractId);

    // ============================
    // DEPOSIT CONTRACT TRANSITIONS
    // ============================

    /**
     * Approve a deposit contract (DRAFT -> WAITING_OFFICIAL).
     * Agent will handle paperwork after this.
     */
    DepositContractDetailResponse approveDepositContract(UUID contractId);

    /**
     * Create payment for the deposit contract customer.
     * Can only be called when contract is in WAITING_OFFICIAL state.
     * Creates payment, calls payment gateway, sends notification.
     */
    DepositContractDetailResponse createDepositPayment(UUID contractId);

    /**
     * Mark paperwork as complete.
     * If payment is pending -> PENDING_PAYMENT
     * If all paid or no payment -> ACTIVE
     */
    DepositContractDetailResponse markDepositPaperworkComplete(UUID contractId);

    /**
     * Cancel deposit contract by customer or owner.
     * - Customer cancels: deposit goes to owner
     * - Owner cancels: deposit returns to customer, owner pays penalty
     */
    DepositContractDetailResponse cancelDepositContract(UUID contractId, CancelDepositContractRequest request);

    /**
     * Void deposit contract (admin only).
     * Just sets to CANCELLED with no side effects.
     */
    DepositContractDetailResponse voidDepositContract(UUID contractId);

    /**
     * Called when deposit payment is completed (from webhook).
     * Transitions from PENDING_PAYMENT -> ACTIVE if all payments are complete.
     */
    void onDepositPaymentCompleted(UUID contractId);

    /**
     * Mark deposit contract as COMPLETED.
     * Called when linked main contract becomes ACTIVE.
     */
    void completeDepositContract(UUID contractId);
}

