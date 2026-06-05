package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.transaction.internal.application.port.in.EscrowUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.EscrowRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentGatewayPort;
import com.se.bds.core.transaction.internal.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService implements EscrowUseCase {

    private final EscrowRepository escrowRepository;
    private final ContractRepository contractRepository;
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    @Transactional
    public EscrowHold createEscrowHold(UUID contractId, UUID paymentId, BigDecimal amount, String description) {
        log.info("[EVENT] Creating escrow hold for contractId={}, paymentId={}, amount={}", contractId, paymentId, amount);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));

        EscrowHold escrowHold = EscrowHold.builder()
                .contractId(contractId)
                .contractType(contract.getContractType())
                .paymentId(paymentId)
                .propertyId(contract.getPropertyId())
                .customerId(contract.getCustomerId())
                .holdAmount(amount)
                .releasedAmount(BigDecimal.ZERO)
                .deductedAmount(BigDecimal.ZERO)
                .status(EscrowStatus.PENDING)
                .description(description)
                .build();

        EscrowHold saved = escrowRepository.save(escrowHold);
        log.info("[EVENT] Escrow hold created: escrowId={}, status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Override
    @Transactional
    public EscrowHold confirmEscrowHold(UUID escrowId) {
        log.info("[EVENT] Confirming escrow hold: escrowId={}", escrowId);
        EscrowHold hold = getHold(escrowId);
        hold.confirmHold();
        EscrowHold saved = escrowRepository.save(hold);
        log.info("[EVENT] Escrow hold confirmed: escrowId={}, status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Override
    @Transactional
    public EscrowHold releaseToOwner(UUID escrowId, UUID adminUserId, String reason) {
        log.info("[ACCOUNTS] Admin={} releasing escrowId={} to owner, reason={}", adminUserId, escrowId, reason);
        EscrowHold hold = getHold(escrowId);
        hold.releaseToOwner(adminUserId, reason);
        EscrowHold saved = escrowRepository.save(hold);

        // TODO: end-to-end escrow hold/release cycle with Stripe sandbox
        
        paymentGatewayPort.createPayoutSession(
                hold.getHoldAmount(),
                "VND",
                hold.getBankAccountNumber() != null ? hold.getBankAccountNumber() : "MOCK_OWNER_ACCOUNT",
                hold.getBankAccountName() != null ? hold.getBankAccountName() : "Owner Account",
                hold.getBankBin() != null ? hold.getBankBin() : "MOCK_BIN",
                "Escrow release for contract " + hold.getContractId(),
                new HashMap<>(),
                escrowId.toString() + "_owner"
        );

        log.info("[EVENT] Escrow released to owner: escrowId={}, amount={}", saved.getId(), saved.getHoldAmount());
        return saved;
    }

    @Override
    @Transactional
    public EscrowHold returnToCustomer(UUID escrowId, UUID adminUserId, String reason) {
        log.info("[ACCOUNTS] Admin={} returning escrowId={} to customer, reason={}", adminUserId, escrowId, reason);
        EscrowHold hold = getHold(escrowId);
        hold.returnToCustomer(adminUserId, reason);
        EscrowHold saved = escrowRepository.save(hold);

        // TODO: end-to-end escrow hold/release cycle with Stripe sandbox

        paymentGatewayPort.createPayoutSession(
                hold.getHoldAmount(),
                "VND",
                hold.getBankAccountNumber() != null ? hold.getBankAccountNumber() : "MOCK_CUSTOMER_ACCOUNT",
                hold.getBankAccountName() != null ? hold.getBankAccountName() : "Customer Account",
                hold.getBankBin() != null ? hold.getBankBin() : "MOCK_BIN",
                "Escrow return for contract " + hold.getContractId(),
                new HashMap<>(),
                escrowId.toString() + "_customer"
        );

        log.info("[EVENT] Escrow returned to customer: escrowId={}, amount={}", saved.getId(), saved.getHoldAmount());
        return saved;
    }

    @Override
    @Transactional
    public EscrowHold partialRelease(UUID escrowId, BigDecimal deductionAmount, UUID adminUserId, String reason) {
        log.info("[ACCOUNTS] Admin={} performing partial release on escrowId={}, deduction={}, reason={}",
                adminUserId, escrowId, deductionAmount, reason);
        EscrowHold hold = getHold(escrowId);
        hold.partialRelease(deductionAmount, adminUserId, reason);
        EscrowHold saved = escrowRepository.save(hold);

        // TODO: end-to-end escrow hold/release cycle with Stripe sandbox

        BigDecimal returnAmount = hold.getReleasedAmount();
        if (returnAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentGatewayPort.createPayoutSession(
                    returnAmount,
                    "VND",
                    hold.getBankAccountNumber() != null ? hold.getBankAccountNumber() : "MOCK_CUSTOMER_ACCOUNT",
                    hold.getBankAccountName() != null ? hold.getBankAccountName() : "Customer Account",
                    hold.getBankBin() != null ? hold.getBankBin() : "MOCK_BIN",
                    "Partial escrow return for contract " + hold.getContractId(),
                    new HashMap<>(),
                    escrowId.toString() + "_partial_cust"
            );
        }
        if (deductionAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentGatewayPort.createPayoutSession(
                    deductionAmount,
                    "VND",
                    "MOCK_OWNER_ACCOUNT",
                    "Owner Account",
                    "MOCK_BIN",
                    "Partial escrow deduction for contract " + hold.getContractId(),
                    new HashMap<>(),
                    escrowId.toString() + "_partial_owner"
            );
        }

        log.info("[EVENT] Escrow partial release executed: escrowId={}, released={}, deducted={}",
                saved.getId(), saved.getReleasedAmount(), saved.getDeductedAmount());
        return saved;
    }

    @Override
    @Transactional
    public EscrowHold forfeit(UUID escrowId, UUID adminUserId, String reason) {
        log.info("[ACCOUNTS] Admin={} forfeiting escrowId={}, reason={}", adminUserId, escrowId, reason);
        EscrowHold hold = getHold(escrowId);
        hold.forfeit(adminUserId, reason);
        EscrowHold saved = escrowRepository.save(hold);

        // TODO: end-to-end escrow hold/release cycle with Stripe sandbox

        paymentGatewayPort.createPayoutSession(
                hold.getHoldAmount(),
                "VND",
                "MOCK_OWNER_ACCOUNT",
                "Owner Account",
                "MOCK_BIN",
                "Escrow forfeit for contract " + hold.getContractId(),
                new HashMap<>(),
                escrowId.toString() + "_forfeit"
        );

        log.info("[EVENT] Escrow forfeited: escrowId={}, amount={}", saved.getId(), saved.getHoldAmount());
        return saved;
    }

    @Override
    public List<EscrowHold> getEscrowHoldsForContract(UUID contractId) {
        return escrowRepository.findByContractId(contractId);
    }

    private EscrowHold getHold(UUID escrowId) {
        return escrowRepository.findById(escrowId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
    }
}
