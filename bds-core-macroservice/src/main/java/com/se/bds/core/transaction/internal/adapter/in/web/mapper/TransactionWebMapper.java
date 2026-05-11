package com.se.bds.core.transaction.internal.adapter.in.web.mapper;

import com.se.bds.core.transaction.internal.adapter.in.web.dto.CreateDepositContractWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.dto.CreatePurchaseContractWebRequest;
import com.se.bds.core.transaction.internal.adapter.in.web.dto.CreateRentalContractWebRequest;
import com.se.bds.core.transaction.internal.application.command.CreateDepositContractCommand;
import com.se.bds.core.transaction.internal.application.command.CreatePurchaseContractCommand;
import com.se.bds.core.transaction.internal.application.command.CreateRentalContractCommand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionWebMapper {
    CreateDepositContractCommand toCreateDepositContractCommand(CreateDepositContractWebRequest createDepositContractWebRequest);
    CreatePurchaseContractCommand toCreatePurchaseContractCommand(CreatePurchaseContractWebRequest createPurchaseContractWebRequest);
    CreateRentalContractCommand toCreateRentalContractCommand(CreateRentalContractWebRequest createRentalContractWebRequest);
}
