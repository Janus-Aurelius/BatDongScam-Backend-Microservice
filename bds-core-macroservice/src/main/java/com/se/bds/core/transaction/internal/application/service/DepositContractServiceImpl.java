package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.transaction.api.event.ContractStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.command.CreateDepositContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class DepositContractServiceImpl implements DepositContractUseCase {
    private final DepositContractRepository depositContractRepository;
    private final PropertyFacade propertyFacade;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param createDepositContractCommand
     * @return
     */
    @Override
    @Transactional
    public DepositContract createDepositContract(CreateDepositContractCommand createDepositContractCommand) {
        propertyFacade.validatePropertyAvailableForContract(new PropertyId(createDepositContractCommand.propertyId()), ContractType.DEPOSIT.name());
        if (depositContractRepository.existsActiveContractForProperty(createDepositContractCommand.propertyId()))
        {
            //TODO: wire error message with SRS
            throw new IllegalArgumentException("Active contract already exist for this property");
        }
        DepositContract contract = new DepositContract();
        contract.setPropertyId(createDepositContractCommand.propertyId());
        contract.setCustomerId(createDepositContractCommand.customerId());
        contract.setAgreedPrice(createDepositContractCommand.agreedPrice());
        contract.setDepositAmount(createDepositContractCommand.depositAmount());
        contract.setStartDate(createDepositContractCommand.expectedSignDate());
        contract.setSpecialTerms(createDepositContractCommand.note());
        contract.setStatus(ContractStatus.DRAFT);
        return depositContractRepository.save(contract);
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public DepositContract approveDepositContract(UUID contractId) {
        DepositContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.WAITING_OFFICIAL);
        DepositContract saved = depositContractRepository.save(contract);
        publishStatusEvent(contract,oldStatus, ContractStatus.WAITING_OFFICIAL);
        return saved;
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public DepositContract markDepositContractPaperworkComplete(UUID contractId) {
        DepositContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.PENDING_PAYMENT);
        DepositContract saved = depositContractRepository.save(contract);
        publishStatusEvent(contract,oldStatus,ContractStatus.PENDING_PAYMENT);
        return saved;
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    public DepositContract cancelDepositContract(UUID contractId, String reason) {
        DepositContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancellationReason(reason);
        //TODO add auth context and check cancellation permisison business context
        contract.setCancelledBy(Role.CUSTOMER);
        DepositContract saved = depositContractRepository.save(contract);
        publishStatusEvent(contract,oldStatus,ContractStatus.CANCELLED);
        return saved;

    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public DepositContract voidDepositContract(UUID contractId) {
        //TODO check eligibility if admin can do to send message accordingly
        return cancelDepositContract(contractId, "Voided by Admin");
    }

    /**
     * @param contractId
     */
    @Override
    @Transactional
    public void deleteDepositContract(UUID contractId) {
        depositContractRepository.delete(getContract(contractId));

    }

    /**
     * @param contractId
     */
    @Override
    @Transactional
    public void completeDepositContract(UUID contractId) {
        DepositContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(ContractStatus.COMPLETED);
        depositContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus,ContractStatus.COMPLETED);
    }

    private void publishStatusEvent(DepositContract contract, ContractStatus oldStatus, ContractStatus contractStatus) {
        eventPublisher.publishEvent(new ContractStatusChangedEvent(
                new ContractId(contract.getId()), ContractType.DEPOSIT.name(), contract.getPropertyId(),
                        oldStatus.name(), contractStatus.name(), Instant.now()
        ));
    }

    private DepositContract getContract(UUID contractId) {
        return depositContractRepository.findById(contractId)
        .orElseThrow(() -> new IllegalArgumentException("Deposit contract with id " + contractId + " does not exist"));
    }

}
