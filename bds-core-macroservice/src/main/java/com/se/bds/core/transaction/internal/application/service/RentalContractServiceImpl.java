package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.property.api.PropertyFacade;
import com.se.bds.core.shared.dto.PropertySnapshot;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.transaction.api.event.ContractStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.command.CreateRentalContractCommand;
import com.se.bds.core.transaction.internal.application.port.in.DepositContractUseCase;
import com.se.bds.core.transaction.internal.application.port.in.RentalContractUseCase;
import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
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
        propertyFacade.validatePropertyAvailableForContract(new PropertyId(command.propertyId()), ContractType.RENTAL.name());

        //1. validate deposit contract if provided
        DepositContract deposit = null;
        if (command.depositContractId() != null) {
            deposit = depositContractRepository.findById(command.depositContractId())
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
        if (deposit != null) {
            rentalContract.setDepositContract(deposit);
        }

        PropertySnapshot property = propertyFacade.getPropertySnapshot(new PropertyId(command.propertyId()));
        BigDecimal commissionRate = property.commissionRate() != null ? property.commissionRate() : BigDecimal.ZERO;
        // For rental, commission is often one month's rent or a percentage of it. 
        // Here we'll use the commissionRate as a percentage of monthlyRentAmount.
        BigDecimal commissionAmount = command.monthlyRentAmount().multiply(commissionRate);
        rentalContract.setCommissionAmount(commissionAmount);
        rentalContract.setLatePaymentPenaltyRate(new BigDecimal("0.05")); // Default 5%

        rentalContract.setMonthlyRentAmount(command.monthlyRentAmount());
        rentalContract.setSecurityDepositAmount(command.securityDepositAmount());
        rentalContract.setMonthCount(command.durationMonths());
        rentalContract.setStartDate(command.expectedStartDate());
        rentalContract.setSpecialTerms(command.note());
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


        //TODO check the business context and permission
        ContractStatus oldStatus = contract.cancel("Voided by admin", Role.ADMIN);

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


        ContractStatus oldStatus = contract.activate();
        RentalContract saved = rentalContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.ACTIVE);

        //3. transition logic to complete the linked deposit contract once rental becomes
        //active (e.g first month rent paid)
        if (contract.getDepositContract() != null)
        {
            depositContractUseCase.completeDepositContract(contract.getDepositContract().getId());
        }
        return saved;
    }

    /**
     * @param contractId
     */
    @Override
    public void completeRentalContract(UUID contractId) {
        RentalContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.complete();
        rentalContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, ContractStatus.COMPLETED);
    }

    private RentalContract getContract(UUID contractId) {
        return rentalContractRepository.findById(contractId)
                //TODO align with error msg SRS
                .orElseThrow(()-> new IllegalArgumentException("Rental contract not found"));
    }

    private RentalContract transitionStatus(UUID contractId, ContractStatus newStatus) {
        RentalContract contract = getContract(contractId);
        ContractStatus oldStatus = contract.transitionTo(newStatus);
        RentalContract saved = rentalContractRepository.save(contract);
        publishStatusEvent(contract, oldStatus, newStatus);
        return saved;
    }

    private void publishStatusEvent(RentalContract contract, ContractStatus oldStatus, ContractStatus newStatus) {
        eventPublisher.publishEvent(new ContractStatusChangedEvent(
                new ContractId(contract.getId()), ContractType.RENTAL.name(), contract.getPropertyId(), oldStatus.name(), newStatus.name(), Instant.now()
        ));
    }

}
