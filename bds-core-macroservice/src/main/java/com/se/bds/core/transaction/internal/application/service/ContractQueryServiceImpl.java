package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.transaction.internal.application.port.in.ContractQueryUseCase;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.ContractType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of ContractQueryUseCase.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractQueryServiceImpl implements ContractQueryUseCase {

    private final ContractRepository contractRepository;

    @Override
    public Contract getContractById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
    }

    @Override
    public Page<Contract> getContracts(
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            ContractStatus status,
            ContractType contractType,
            Pageable pageable) {
        return contractRepository.findAllWithFilters(customerId, agentId, propertyId, status, contractType, pageable);
    }
}
