package com.se.bds.core.transaction.api;


import com.se.bds.core.shared.dto.RevenuePaymentSnapshot;
import com.se.bds.core.transaction.api.dto.ContractHistoryDataPoint;

import java.util.List;
import java.util.UUID;

//the only way oher module interact with the transaction bounded context
//return read only dtos from the shared kernel
public interface TransactionFacade {
    //property module integrations
    List<ContractHistoryDataPoint> getContractHistoryForProperty(UUID propertyId, boolean includePastContracts);

    boolean hasActiveContractForProperty (UUID propertyId, String contractType);

    //report module
    //replace contractrepository.countSignedInMonth()
    int countContractSignedInMonth(int month, int year);

    //replace paymentRepository.findRevenuePaymentsInMonth
    List<RevenuePaymentSnapshot> getRevenuePaymentsInMonth(int month, int year);

    //agent reviews integration
    com.se.bds.core.transaction.api.dto.AgentReviewSummary getAgentReviewSummary(UUID agentId);
}
