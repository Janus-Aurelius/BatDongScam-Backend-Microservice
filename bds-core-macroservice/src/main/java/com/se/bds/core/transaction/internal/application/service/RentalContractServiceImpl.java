package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.transaction.api.event.ContractStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.command.CreateRentalContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.application.port.in.RentalContractUseCase;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;
import com.se.bds.core.transaction.internal.domain.model.RentalContract;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RentalContractServiceImpl implements RentalContractUseCase {
    private final RentalContractRepository rentalContractRepository;
    private final DepositContractRepository depositContractRepository;
    private final DepositContractUseCase depositContractUseCase;
    private final PropertyFacade propertyFacade;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param command
     * @return
     */
    @Override
    @Transactional
    public RentalContract createRentalContract(CreateRentalContractCommand command) {
        propertyFacade.validatePropertyAvailableForContract(new PropertyId(command.propertyId()), ContractType.RENTAL);

        //1. validate deposit contract if provided
        if (command.depositContractId() != null) {
            DepositContract deposit = depositContractRepository.findById(command.depositContractId())
                    .orElseThrow (() -> new IllegalArgumentException("Deposit contract not found"));
            if (deposit.getStatus() != ContractStatus.ACTIVE)
            {
                throw new IllegalStateException("Deposit contract not active");
            }
            if (!deposit.getPropertyId().equals(command.propertyId()))
            {
                throw new IllegalArgumentException("Deposit contract property not match with property id");
            }
            if (!deposit.getCustomerId().equals(command.customerId()))
            {
                throw new IllegalArgumentException("Deposit contract customer not match with customer id");
            }
            //TODO checking if this logic is corect
            if (deposit.getAgreedPrice().compareTo(command.monthlyRentAmount()) != 0)
            {
                throw new IllegalArgumentException("Monthly rent amount must match the deposit contract's agreed price");
            }
        }
        RentalContract rentalContract = new RentalContract();
        rentalContract.setCustomerId(command.customerId());
        rentalContract.setPropertyId(command.propertyId());
        rentalContract.setDepositContractId(command.depositContractId());
        rentalContract.setMonthlyRentAmount(command.monthlyRentAmount());
        rentalContract.setSecurityDepositAmount(command.securityDepositAmount());
        rentalContract.setDurationMonth(command.durationMonths());
        rentalContract.setPaymentCycleMonths(command.paymentCycleMonths());
        rentalContract.setExpectedStartDate(command.expectedStartDate());
        rentalContract.setNote(command.note());
        rentalContract.setStatus(ContractStatus.DRAFT);

        return rentalContractRepository.save(rentalContract);

    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public RentalContract approveRentalContract(UUID contractId) {
        return transitionStatus(contractId, ContractStatus.WAITING_OFFICIAL);
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public RentalContract markRentalContractPaperworkComplete(UUID contractId) {
        return transitionStatus(contractId,ContractStatus.PENDING_PAYMENT);
    }

    /**
     * @param contractId
     * @param decision
     * @param deductionAmount
     * @param reason
     * @return
     */
    @Override
    @Transactional
    public RentalContract decideSecurityDeposit(UUID contractId, String decision, BigDecimal deductionAmount, String reason) {
        RentalContract contract = getContract(contractId);
        //TODO        Do the Implementation for security deposit settlement logic
        //        // This is typically done post-COMPLETED or during termination
        return contract;
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    @Transactional
    public RentalContract voidRentalContract(UUID contractId) {
        RentalContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();

        contract.setStatus(ContractStatus.CANCELLED);
        //TODO check the business context and permission
        contract.setCancellationReason("Voided by admin");
        contract.setCancelledBy(Role.ADMIN);

        RentalContract saved = rentalContractRepository.save(contract);
        publishStatusEvent(contract,oldStatus,ContractStatus.CANCELLED);
        return saved;
    }

    /**
     * @param contractId
     * @return
     */
    @Override
    public RentalContract activateRentalContract(UUID contractId) {
        RentalContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();

        contract.setStatus(ContractStatus.ACTIVE);
        RentalContract saved = rentalContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.ACTIVE);

        //3. transition logic to complete the linked deposit contract once rental becomes
        //active (e.g first month rent paid)
        if (contract.getDepositContractId() != null)
        {
            depositContractUseCase.completeDepositContract(contract.getDepositContractId());
        }
        return saved;
    }

    /**
     * @param contractId
     */
    @Override
    public void completeRentalContract(UUID contractId) {
        RentalContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.getStatus();

        contract.setStatus(ContractStatus.COMPLETED);
        rentalContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.COMPLETED);
    }

    private RentalContract getContract(UUID contractId) {
        return rentalContractRepository.findById(contractId)
                //TODO align with error msg SRS
                .orElseThrow(()-> new IllegalArgumentException("Rental contract not found"))
    }

    private void publishStatusEvent(RentalContract contract, ContractStatus oldStatus, ContractStatus contractStatus) {
        eventPublisher.publishEvent(new ContractStatusChangedEvent(
                new ContractId(contract.getId(), ContractType.RENTAL, contract.getPropertyId(), oldStatus,newStatus, Instant.now())
        ));
    }

}
