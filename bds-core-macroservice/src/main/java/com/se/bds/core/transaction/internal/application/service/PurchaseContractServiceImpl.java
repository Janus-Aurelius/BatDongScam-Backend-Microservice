package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.transaction.api.event.ContractStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.command.CreatePurchaseContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.application.port.in.PurchaseContractUseCase;
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

@Service
@RequiredArgsConstructor
public class PurchaseContractServiceImpl implements PurchaseContractUseCase {
    private final PurchaseContractRepository purchaseContractRepository;
    private final PropertyFacade propertyFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final ContractType contractType;
    private final DepositContractUseCase depositContractUseCase;


    /**
     * @param command
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract createPurchaseContract(CreatePurchaseContractCommand command) {
        propertyFacade.validatePropertyAvailableForContract(new PropertyId(command.propertyId()), contractType.PURCHASE);

        if (purchaseContractRepository.existActiveContractForProperty(command.propertyId()))
        {
            throw new IllegalArgumentException("Active contract already exists for this property");
        }
        // 1. transition logic
        // check for deposit contract
        if (command.depositContractId()!=null)
        {
            DepositContract deposit = depositContractRepository.findById(command.depositContractId())
                    //TODO align error msg with SRS
                    .orElseThrow(() -> new IllegalArgumentException("Deposit contract not found"));
            if (deposit.getStatus() != ContractStatus.ACTIVE)
            {
                throw new IllegalStateException("Deposit contract is not ACTIVE, it must be ACTIVE");
            }
            if (!Objects.equals(deposit.getPropertyId(),command.propertyId()))
            {
                throw new IllegalArgumentException("Deposit contract does not match property id");
            }
            if (!Objects.equals(deposit.getCustomerId(),command.customerId()))
            {
                throw new IllegalArgumentException("Deposit contract does not match customer id");
            }
            if (deposit.getAgreedPrice().compareTo(command.agreedPrice()) != 0)
            {
                throw new IllegalArgumentException("Agreed contract price does not match agreed price");
            }
        }
        PurchaseContract contract = new PurchaseContract();
        contract.setPropertyId(command.propertyId());
        contract.setCustomerId(command.customerId());
        contract.setDepositContractId(command.depositContractId());
        contract.setAdvancePaymentAmount(command.advancePaymentAmount());
        contract.setAdvancePaymentDeadline(command.advancePaymentDeadline());
        contract.setFinalPaymentDeadline(command.setFinalPaymentDeadline());
        contract.setNote(command.setNote());
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
        ContractStatus oldStatus = contract.getStatus();

        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancellationReason(reason);
        // TODO check business logic for who can cancel the contract
        contract.setCancelledBy(Role.CUSTOMER);
        publishStatusEvent(contract,oldStatus, ContractStatus.CANCELLED);
        return saved;
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public PurchaseContract voidPurchaseContract(UUID contractId) {
        //TODO check business context of who can make the decision
        return cancelPurchaseContract(contractId, "Voided by Admin");
    }

    /**
     * @param contractId
     */
    @Override
    @Transactional
    public void completePurchaseContract(UUID contractId) {
        PurchaseContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.COMPLETED);
        purchaseContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.COMPLETED);
        // 2. transition logic to complete the linked
        // deposit contract once purchases is completed
        if (contract.getDepositContractId(contractId) != null)
        {
            depositContractUseCase.completeDepositContract(contract.getDepositContractId());
        }
    }
    private PurchaseContract transitionStatus(UUID contractId, ContractStatus newStatus) {
        PurchaseContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(newStatus);
        PurchaseContract saved = purchaseContractRepository.save(contract);
        publishStatusEvent(contract,oldStatus,newStatus.name());
        return saved;
    }


    private PurchaseContract getContract(UUID contractId) {
        return purchaseContractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase contract not found"));
    }
    private void publishStatusEvent(PurchaseContract contract, ContractStatus oldStatus, String name) {
        eventPublisher.publishEvent(new ContractStatusChangedEvent(new ContractId(contract.getId()),
                ContractType.PURCHASE, contract.getPropertyId(),oldStatus,newStatus, Instant.now()));
    }
}
