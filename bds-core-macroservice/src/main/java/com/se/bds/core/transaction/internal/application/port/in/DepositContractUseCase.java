package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.application.command.CreateDepositContractCommand;
import com.se.bds.core.transaction.internal.domain.model.DepositContract;

import java.util.UUID;

public interface DepositContractUseCase {
    DepositContract createDepositContract(CreateDepositContractCommand createDepositContractCommand);
    DepositContract approveDepositContract(UUID contractId);
    DepositContract markDepositContractPaperworkComplete (UUID contractId);
    DepositContract cancelDepositContract(UUID contractId, String reason);
    DepositContract voidDepositContract (UUID contractId);
    void deleteDepositContract(UUID contractId);
    void completeDepositContract(UUID contractId);
}
