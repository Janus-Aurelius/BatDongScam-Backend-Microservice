package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.command.CreateDepositContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import com.se.bds.core.transaction.internal.application.port.out.UserValidationPort;
import com.se.bds.core.shared.dto.PropertySnapshot;
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
    private final UserValidationPort userValidationPort;

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
            throw new BusinessException(MSG12.CODE, "Active contract already exist for this property");
        }

        userValidationPort.validateCustomer(createDepositContractCommand.customerId());
        PropertySnapshot property = propertyFacade.getPropertySnapshot(new PropertyId(createDepositContractCommand.propertyId()));
        if (property.assignedAgentId() != null) {
            userValidationPort.validateAgent(property.assignedAgentId());
        }

        DepositContract contract = new DepositContract();
        contract.setPropertyId(createDepositContractCommand.propertyId());
        contract.setCustomerId(createDepositContractCommand.customerId());
        if (property.assignedAgentId() != null) {
            contract.setAgentId(property.assignedAgentId());
        }
        contract.setMainContractType(createDepositContractCommand.mainContractType());
        contract.setAgreedPrice(createDepositContractCommand.agreedPrice());
        contract.setDepositAmount(createDepositContractCommand.depositAmount());
        contract.setStartDate(createDepositContractCommand.expectedSignDate());
        contract.setSpecialTerms(createDepositContractCommand.note());
        contract.transitionTo(ContractStatus.DRAFT);
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
        ContractStatus oldStatus = contract.transitionTo(ContractStatus.WAITING_OFFICIAL);
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
        ContractStatus oldStatus = contract.transitionTo(ContractStatus.PENDING_PAYMENT);
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
        //TODO add auth context and check cancellation permisison business context
        ContractStatus oldStatus = contract.cancel(reason,Role.CUSTOMER);
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
        ContractStatus oldStatus = contract.complete();
        depositContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus,ContractStatus.COMPLETED);
    }

    private void publishStatusEvent(DepositContract contract, ContractStatus oldStatus, ContractStatus contractStatus) {
        eventPublisher.publishEvent(new ContractStatusChangedEvent(
                new ContractId(contract.getId()), ContractType.DEPOSIT.name(), contract.getPropertyId(),
                        contract.getCustomerId(), oldStatus.name(), contractStatus.name(), Instant.now()
        ));
    }

    private DepositContract getContract(UUID contractId) {
        return depositContractRepository.findById(contractId)
        .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
    }
}
