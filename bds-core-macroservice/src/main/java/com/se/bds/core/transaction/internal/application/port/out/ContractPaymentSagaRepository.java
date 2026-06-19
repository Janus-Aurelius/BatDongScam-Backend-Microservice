package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.ContractPaymentSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Repository interface to persist and query saga states.
 */
public interface ContractPaymentSagaRepository extends JpaRepository<ContractPaymentSaga, UUID> {
}
