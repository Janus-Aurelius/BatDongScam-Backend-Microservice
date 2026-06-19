package com.se.bds.core.transaction.internal.application.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.event.SagaPaymentCommand;
import com.se.bds.common.event.SagaPaymentResult;
import com.se.bds.core.transaction.internal.application.port.out.ContractPaymentSagaRepository;
import com.se.bds.core.transaction.internal.application.port.out.ContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.Contract;
import com.se.bds.core.transaction.internal.domain.model.ContractPaymentSaga;
import com.se.bds.core.transaction.internal.domain.model.ContractStatus;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.core.transaction.internal.domain.model.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractPaymentSagaOrchestratorTest {

    @Mock
    private ContractPaymentSagaRepository sagaRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private KafkaOperations<String, String> kafkaTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private ContractPaymentSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        orchestrator = new ContractPaymentSagaOrchestrator(
                sagaRepository,
                contractRepository,
                paymentRepository,
                kafkaTemplate,
                objectMapper,
                eventPublisher
        );
    }

    @Test
    void startSaga_Success_EmitsPaymentCommand() throws Exception {
        UUID contractId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.00");

        Contract contract = mock(Contract.class);
        when(contract.getStatus()).thenReturn(ContractStatus.PENDING_PAYMENT);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        Payment payment = mock(Payment.class);
        when(payment.getAmount()).thenReturn(amount);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        UUID sagaId = orchestrator.startSaga(contractId, paymentId);

        assertNotNull(sagaId);

        // Verify saga entity saved twice (STARTED, then PATENT_PENDING)
        verify(sagaRepository, times(2)).save(any(ContractPaymentSaga.class));

        // Verify Kafka command dispatched
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("contract-saga-commands"), eq(sagaId.toString()), payloadCaptor.capture());

        SagaPaymentCommand command = objectMapper.readValue(payloadCaptor.getValue(), SagaPaymentCommand.class);
        assertEquals(sagaId, command.sagaId());
        assertEquals(contractId, command.contractId());
        assertEquals(paymentId, command.paymentId());
        assertEquals("PROCESS_PAYMENT", command.commandType());
        assertEquals(amount, command.amount());
    }

    @Test
    void handlePaymentResult_Success_ActivatesContract() {
        UUID sagaId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        ContractPaymentSaga saga = ContractPaymentSaga.builder()
                .id(sagaId)
                .contractId(contractId)
                .paymentId(paymentId)
                .status(SagaStatus.PAYMENT_PENDING)
                .amount(new BigDecimal("1500.00"))
                .build();

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Contract contract = mock(Contract.class);
        when(contract.getId()).thenReturn(contractId);
        when(contract.getContractType()).thenReturn(com.se.bds.core.transaction.internal.domain.model.ContractType.RENTAL);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        SagaPaymentResult result = new SagaPaymentResult(sagaId, paymentId, "SUCCESS", "stripe-tx-123", null);

        orchestrator.handlePaymentResult(result);

        // Verify local payment updated to SUCCESS
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("stripe-tx-123", payment.getTransactionReference());
        verify(paymentRepository, times(1)).save(payment);

        // Verify contract activated
        verify(contract, times(1)).transitionTo(ContractStatus.ACTIVE);
        verify(contractRepository, times(1)).save(contract);
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));

        // Verify Saga status updated to CONTRACT_ACTIVE
        assertEquals(SagaStatus.CONTRACT_ACTIVE, saga.getStatus());
        verify(sagaRepository, atLeastOnce()).save(saga);
    }

    @Test
    void handlePaymentResult_ContractActivationFails_TriggersCompensation() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.00");

        ContractPaymentSaga saga = ContractPaymentSaga.builder()
                .id(sagaId)
                .contractId(contractId)
                .paymentId(paymentId)
                .status(SagaStatus.PAYMENT_PENDING)
                .amount(amount)
                .build();

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Contract contract = mock(Contract.class);
        when(contract.getId()).thenReturn(contractId);
        // Throw exception when trying to activate
        doThrow(new RuntimeException("Conflict: property already reserved")).when(contract).transitionTo(ContractStatus.ACTIVE);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        SagaPaymentResult result = new SagaPaymentResult(sagaId, paymentId, "SUCCESS", "stripe-tx-123", null);

        orchestrator.handlePaymentResult(result);

        // Verify local payment status still updated to SUCCESS (since money was taken)
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);

        // Verify contract rolled back (cancelled/failed)
        verify(contract, times(1)).setStatus(ContractStatus.CANCELLED);
        verify(contractRepository, times(1)).save(contract);

        // Verify Saga state transitions to REFUND_PENDING
        assertEquals(SagaStatus.REFUND_PENDING, saga.getStatus());
        verify(sagaRepository, atLeastOnce()).save(saga);

        // Verify compensating REFUND_PAYMENT command sent to Kafka
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("contract-saga-commands"), eq(sagaId.toString()), payloadCaptor.capture());

        SagaPaymentCommand command = objectMapper.readValue(payloadCaptor.getValue(), SagaPaymentCommand.class);
        assertEquals(sagaId, command.sagaId());
        assertEquals("REFUND_PAYMENT", command.commandType());
        assertEquals(amount, command.amount());
    }

    @Test
    void handleRefundResult_Success_CompensatesSaga() {
        UUID sagaId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        ContractPaymentSaga saga = ContractPaymentSaga.builder()
                .id(sagaId)
                .contractId(contractId)
                .paymentId(paymentId)
                .status(SagaStatus.REFUND_PENDING)
                .amount(new BigDecimal("1500.00"))
                .build();

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        SagaPaymentResult result = new SagaPaymentResult(sagaId, paymentId, "REFUNDED", "stripe-tx-123", null);

        orchestrator.handleRefundResult(result);

        // Verify local payment updated to FAILED/REFUNDED
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);

        // Verify Saga status updated to COMPENSATED
        assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        verify(sagaRepository, times(1)).save(saga);
    }
}
