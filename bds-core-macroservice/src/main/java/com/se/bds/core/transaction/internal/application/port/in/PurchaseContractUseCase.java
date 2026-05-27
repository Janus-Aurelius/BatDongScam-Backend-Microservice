package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.application.command.CreatePurchaseContractCommand;
import com.se.bds.core.transaction.internal.domain.model.PurchaseContract;

import java.util.UUID;

public interface PurchaseContractUseCase {
    PurchaseContract createPurchaseContract(CreatePurchaseContractCommand command);
    PurchaseContract approvePurchaseContract(UUID contractId);
    PurchaseContract markPurchaseContractPaperworkComplete(UUID contractId);
    PurchaseContract cancelPurchaseContract(UUID contractId);
    PurchaseContract voidPurchaseContract(UUID contractId);

    //final state transition which trigger deposit completion
    void completePurchaseContract(UUID contractId);
}
