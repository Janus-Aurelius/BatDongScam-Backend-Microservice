package com.se.bds.core.transaction.internal.adapter;

import com.se.bds.core.shared.dto.RevenuePaymentSnapshot;
import com.se.bds.core.transaction.api.TransactionFacade;
import com.se.bds.core.transaction.api.dto.ContractHistoryDataPoint;
import com.se.bds.core.transaction.api.dto.AgentReviewSummary;
import com.se.bds.core.transaction.internal.application.port.in.AgentReviewUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PurchaseContractRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionFacadeImpl implements TransactionFacade {

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final RentalContractRepository rentalContractRepository;
    private final DepositContractRepository depositContractRepository;
    private final PurchaseContractRepository purchaseContractRepository;
    private final AgentReviewUseCase agentReviewUseCase;

    @Override
    public List<ContractHistoryDataPoint> getContractHistoryForProperty(UUID propertyId, boolean includePastContracts) {
        List<Contract> contracts = contractRepository.findByPropertyId(propertyId);
        
        return contracts.stream()
                .filter(contract -> includePastContracts || 
                        (contract.getStatus() != ContractStatus.COMPLETED && contract.getStatus() != ContractStatus.CANCELLED))
                .map(contract -> new ContractHistoryDataPoint(
                        contract.getStartDate(),
                        contract.getEndDate(),
                        contract.getStatus().name()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasActiveContractForProperty(UUID propertyId, String contractType) {
        if (contractType == null) {
            return false;
        }
        try {
            ContractType type = ContractType.valueOf(contractType.toUpperCase());
            switch (type) {
                case DEPOSIT:
                    return depositContractRepository.existsActiveContractForProperty(propertyId);
                case RENTAL:
                    return rentalContractRepository.existsActiveContractForProperty(propertyId);
                case PURCHASE:
                    return purchaseContractRepository.existsActiveContractForProperty(propertyId);
                default:
                    return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public int countContractSignedInMonth(int month, int year) {
        return (int) contractRepository.countSignedInMonth(month, year);
    }

    @Override
    public List<RevenuePaymentSnapshot> getRevenuePaymentsInMonth(int month, int year) {
        List<Payment> payments = paymentRepository.findRevenuePaymentsInMonth(month, year);
        
        return payments.stream()
                .map(payment -> new RevenuePaymentSnapshot(
                        payment.getId(),
                        payment.getContract() != null ? payment.getContract().getId() : null,
                        payment.getPropertyId() != null ? payment.getPropertyId() : 
                                (payment.getContract() != null ? payment.getContract().getPropertyId() : null),
                        payment.getPaymentType() != null ? payment.getPaymentType().name() : null,
                        payment.getAmount(),
                        payment.getPaidTime() != null ? payment.getPaidTime().atZone(ZoneId.systemDefault()).toInstant() : null
                ))
                .collect(Collectors.toList());
    }

    @Override
    public AgentReviewSummary getAgentReviewSummary(UUID agentId) {
        AgentReviewUseCase.AgentReviewSummary summary = agentReviewUseCase.getAgentReviewSummary(agentId);
        return new AgentReviewSummary(
                summary.agentId(),
                summary.averageRating(),
                summary.totalReviews()
        );
    }
}
