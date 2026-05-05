package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.application.command.CreateRentalContractCommand;
import com.se.bds.core.transaction.internal.domain.model.RentalContract;

import java.math.BigDecimal;
import java.util.UUID;

public interface RentalContractUseCase {
    RentalContract createRentalContract(CreateRentalContractCommand command);
    RentalContract approveRentalContract(UUID contractId);
    RentalContract markRentalContractPaperworkComplete(UUID contractId);
    RentalContract decideSecurityDeposit(UUID contractId, String decision, BigDecimal deductionAmount, String reason);
    RentalContract voidRentalContract(UUID contractId);

    // final transition to active triggering deposit completion
    RentalContract activateRentalContract(UUID contractId);
    void completeRentalContract(UUID contractId);
}
