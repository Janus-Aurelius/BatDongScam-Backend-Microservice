package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG13;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.dto.PropertySnapshot;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.command.CreatePurchaseContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.application.port.in.PurchaseContractUseCase;
import com.se.bds.core.transaction.internal.application.port.out.PurchaseContractRepository;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import com.se.bds.core.transaction.internal.domain.model.PurchaseContract;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.se.bds.core.transaction.internal.application.port.out.UserValidationPort;

@Service
@RequiredArgsConstructor
public class PurchaseContractServiceImpl implements PurchaseContractUseCase {
    private final PurchaseContractRepository purchaseContractRepository;
    private final PropertyFacade propertyFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final DepositContractUseCase depositContractUseCase;
    private final com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository depositContractRepository;
    private final UserValidationPort userValidationPort;


    /**
     * @param command
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract createPurchaseContract(CreatePurchaseContractCommand command) {
        propertyFacade.validatePropertyAvailableForContract(new PropertyId(command.propertyId()), ContractType.PURCHASE.name());

        if (purchaseContractRepository.existsActiveContractForProperty(command.propertyId()))
        {
            throw new BusinessException(MSG12.CODE, "Active contract already exists for this property");
        }

        userValidationPort.validateCustomer(command.customerId());
        PropertySnapshot property = propertyFacade.getPropertySnapshot(new PropertyId(command.propertyId()));
        if (property.assignedAgentId() != null) {
            userValidationPort.validateAgent(property.assignedAgentId());
        }

        // 1. transition logic
        // check for deposit contract
        DepositContract deposit = null;
        if (command.depositContractId()!=null)
        {
            deposit = depositContractRepository.findById(command.depositContractId())
                    .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
            if (deposit.getStatus() != ContractStatus.ACTIVE)
            {
                throw new BusinessException(MSG12.CODE, "Deposit contract is not ACTIVE, it must be ACTIVE");
            }
            if (!Objects.equals(deposit.getPropertyId(),command.propertyId()))
            {
                throw new BusinessException(MSG12.CODE, "Deposit contract does not match property id");
            }
            if (!Objects.equals(deposit.getCustomerId(),command.customerId()))
            {
                throw new BusinessException(MSG12.CODE, "Deposit contract does not match customer id");
            }
            if (deposit.getAgreedPrice().compareTo(command.agreedPrice()) != 0)
            {
                throw new BusinessException(MSG13.CODE, "Agreed contract price does not match agreed price");
            }
        }
        PurchaseContract contract = new PurchaseContract();
        contract.setPropertyId(command.propertyId());
        contract.setCustomerId(command.customerId());
        if (property.assignedAgentId() != null) {
            contract.setAgentId(property.assignedAgentId());
        }
        if (deposit != null) {
            contract.setDepositContract(deposit);
        }

        java.math.BigDecimal commissionRate = property.commissionRate() != null ? property.commissionRate() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal commissionAmount = command.agreedPrice().multiply(commissionRate);
        contract.setCommissionAmount(commissionAmount);

        contract.setPropertyValue(command.agreedPrice());
        contract.setAdvancePaymentAmount(command.advancePaymentAmount());
        if (command.advancePaymentDeadline() != null) contract.setStartDate(command.advancePaymentDeadline());
        if (command.finalPaymentDeadline() != null) contract.setEndDate(command.finalPaymentDeadline());
        contract.setSpecialTerms(command.note());
        contract.setStatus(ContractStatus.DRAFT);
        return purchaseContractRepository.save(contract);
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract approvePurchaseContract(UUID contractId) {
        return transitionStatus(contractId, ContractStatus.WAITING_OFFICIAL);
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract markPurchaseContractPaperworkComplete(UUID contractId) {
        return transitionStatus(contractId,ContractStatus.PENDING_PAYMENT);
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract cancelPurchaseContract(UUID contractId) {
        PurchaseContract contract = getContract(contractId);
        // TODO check business logic for who can cancel the contract

        ContractStatus oldStatus = contract.cancel("Cancelled by customer", Role.CUSTOMER);
        PurchaseContract saved = purchaseContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.CANCELLED);
        return saved;
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract voidPurchaseContract(UUID contractId) {
        PurchaseContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.cancel("Voided by admin", Role.ADMIN);
        PurchaseContract saved = purchaseContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.CANCELLED);
        return saved;
    }

    /**
     * @param contractId
     */
    @Override
    @Transactional
    public void completePurchaseContract(UUID contractId) {
        PurchaseContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.complete();
        purchaseContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.COMPLETED);
        // 2. transition logic to complete the linked
        // deposit contract once purchases is completed
        if (contract.getDepositContract() != null)
        {
            depositContractUseCase.completeDepositContract(contract.getDepositContract().getId());
        }
    }
    private PurchaseContract transitionStatus(UUID contractId, ContractStatus newStatus) {
        PurchaseContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.transitionTo(newStatus);
        PurchaseContract saved = purchaseContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, newStatus);
        return saved;
    }


    private PurchaseContract getContract(UUID contractId) {
        return purchaseContractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
    }
    private void publishStatusEvent(PurchaseContract contract, ContractStatus oldStatus, ContractStatus newStatus) {
        eventPublisher.publishEvent(new ContractStatusChangedEvent(new ContractId(contract.getId()),
                ContractType.PURCHASE.name(), contract.getPropertyId(), oldStatus.name(), newStatus.name(), Instant.now()));
    }
}
