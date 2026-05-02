package com.se100.bds.services.domains.contract.impl;

import com.se100.bds.dtos.requests.contract.CancelPurchaseContractRequest;
import com.se100.bds.dtos.requests.contract.CreatePurchaseContractRequest;
import com.se100.bds.dtos.requests.contract.UpdatePurchaseContractRequest;
import com.se100.bds.dtos.responses.contract.PurchaseContractDetailResponse;
import com.se100.bds.dtos.responses.contract.PurchaseContractDetailResponse.PaymentSummary;
import com.se100.bds.dtos.responses.contract.PurchaseContractDetailResponse.PropertySummary;
import com.se100.bds.dtos.responses.contract.PurchaseContractDetailResponse.UserSummary;
import com.se100.bds.dtos.responses.contract.PurchaseContractListItem;
import com.se100.bds.exceptions.BadRequestException;
import com.se100.bds.exceptions.ForbiddenException;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.entities.contract.PurchaseContract;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.contract.DepositContractRepository;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.repositories.domains.contract.PurchaseContractRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.services.domains.contract.DepositContractService;
import com.se100.bds.services.domains.contract.PurchaseContractService;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.domains.report.FinancialUpdateService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.payment.PaymentGatewayService;
import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.services.payment.dto.CreatePayoutSessionRequest;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import com.se100.bds.utils.Constants.MainContractTypeEnum;
import com.se100.bds.utils.Constants.NotificationTypeEnum;
import com.se100.bds.utils.Constants.PaymentStatusEnum;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import com.se100.bds.utils.Constants.RelatedEntityTypeEnum;
import com.se100.bds.utils.Constants.RoleEnum;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseContractServiceImpl implements PurchaseContractService {

    private final PurchaseContractRepository purchaseContractRepository;
    private final DepositContractRepository depositContractRepository;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final UserService userService;
    private final PaymentGatewayService paymentGatewayService;
    private final NotificationService notificationService;
    private final DepositContractService depositContractService;

    private static final int DEFAULT_PAYMENT_DUE_DAYS = 7;
    private static final String CURRENCY_VND = "VND";
    private static final String PAYOS_METHOD = "PAYOS";
    private final FinancialUpdateService financialUpdateService;

    // ========================
    // PURCHASE CONTRACT CRUD
    // ========================

    @Override
    @Transactional
    public PurchaseContractDetailResponse createPurchaseContract(CreatePurchaseContractRequest request) {
        boolean isAdmin = hasRole(RoleEnum.ADMIN);
        boolean isAgent = hasRole(RoleEnum.SALESAGENT);

        if (!isAdmin && !isAgent) {
            throw new ForbiddenException("Only admins and sales agents can create purchase contracts");
        }

        // Validate property exists
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NotFoundException("Property not found: " + request.getPropertyId()));

        // Check no non-DRAFT purchase contract exists for this property
        if (purchaseContractRepository.existsNonDraftPurchaseContractForProperty(
                request.getPropertyId(), ContractStatusEnum.DRAFT)) {
            throw new BadRequestException("A non-draft purchase contract already exists for this property. " +
                    "Only one active purchase contract is allowed per property.");
        }

        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));

        // Validate commission < property value
        if (request.getCommissionAmount().compareTo(request.getPropertyValue()) >= 0) {
            throw new BadRequestException("Commission amount must be less than property value");
        }

        // Validate deposit contract if provided
        DepositContract depositContract = null;
        if (request.getDepositContractId() != null) {
            depositContract = depositContractRepository.findById(request.getDepositContractId())
                    .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + request.getDepositContractId()));

            // Deposit must be ACTIVE
            if (depositContract.getStatus() != ContractStatusEnum.ACTIVE) {
                throw new BadRequestException("Deposit contract must be in ACTIVE status");
            }

            // Deposit must not be expired
            if (depositContract.getEndDate() != null && depositContract.getEndDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Deposit contract has expired");
            }

            // Deposit must be for PURCHASE type
            if (depositContract.getMainContractType() != MainContractTypeEnum.PURCHASE) {
                throw new BadRequestException("Deposit contract must be for PURCHASE type");
            }

            // Property must match
            if (!depositContract.getProperty().getId().equals(request.getPropertyId())) {
                throw new BadRequestException("Deposit contract property does not match");
            }

            // Customer must match
            if (!depositContract.getCustomer().getId().equals(request.getCustomerId())) {
                throw new BadRequestException("Deposit contract customer does not match");
            }

            // Property value must match agreed price
            if (request.getPropertyValue().compareTo(depositContract.getAgreedPrice()) != 0) {
                throw new BadRequestException("Property value must match deposit contract's agreed price: " +
                        depositContract.getAgreedPrice());
            }
        }

        // Determine agent
        SaleAgent agent;
        if (isAdmin) {
            if (request.getAgentId() == null) {
                throw new BadRequestException("Agent ID is required when admin creates contract");
            }
            agent = saleAgentRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new NotFoundException("Agent not found: " + request.getAgentId()));
        } else {
            UUID currentUserId = userService.getUserId();
            agent = saleAgentRepository.findById(currentUserId)
                    .orElseThrow(() -> new NotFoundException("Current agent not found"));
        }

        // Create the purchase contract
        PurchaseContract contract = new PurchaseContract();
        contract.setProperty(property);
        contract.setCustomer(customer);
        contract.setAgent(agent);
        contract.setStatus(ContractStatusEnum.DRAFT);
        contract.setDepositContract(depositContract);
        contract.setPropertyValue(request.getPropertyValue());
        contract.setAdvancePaymentAmount(request.getAdvancePaymentAmount() != null ? request.getAdvancePaymentAmount() : BigDecimal.ZERO);
        contract.setCommissionAmount(request.getCommissionAmount());
        contract.setStartDate(request.getStartDate());
        contract.setSpecialTerms(request.getSpecialTerms());

        PurchaseContract saved = purchaseContractRepository.save(contract);
        log.info("Created purchase contract {} for property {}", saved.getId(), property.getId());

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseContractDetailResponse getPurchaseContractById(UUID contractId) {
        PurchaseContract contract = purchaseContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        checkReadAccess(contract);

        return mapToDetailResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseContractListItem> getPurchaseContracts(
            Pageable pageable,
            List<ContractStatusEnum> statuses,
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            String search
    ) {
        boolean isAdmin = hasRole(RoleEnum.ADMIN);
        boolean isAgent = hasRole(RoleEnum.SALESAGENT);
        boolean isCustomer = hasRole(RoleEnum.CUSTOMER);
        boolean isOwner = hasRole(RoleEnum.PROPERTY_OWNER);
        UUID currentUserId = userService.getUserId();

        // Authorization logic:
        // - Admin can query all
        // - Agent can query their assigned contracts (will be filtered by agentId in spec)
        // - Customer can query only their own contracts (customerId must match current user)
        // - Owner can query only contracts for their properties (ownerId must match current user)
        
        if (!isAdmin && !isAgent) {
            if (isCustomer && customerId != null && customerId.equals(currentUserId)) {
                // Customer querying their own contracts - allowed
            } else if (isOwner && ownerId != null && ownerId.equals(currentUserId)) {
                // Owner querying their property contracts - allowed
            } else {
                throw new ForbiddenException("You don't have permission to query these contracts");
            }
        }

        Specification<PurchaseContract> spec = buildPurchaseContractSpecification(
                statuses, customerId, agentId, propertyId, ownerId,
                startDateFrom, startDateTo, search, isAdmin, isAgent
        );

        return purchaseContractRepository.findAll(spec, pageable).map(this::mapToListItem);
    }

    @Override
    @Transactional
    public PurchaseContractDetailResponse updatePurchaseContract(UUID contractId, UpdatePurchaseContractRequest request) {
        PurchaseContract contract = purchaseContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be updated");
        }

        checkWriteAccess(contract);

        // Update fields if provided
        if (request.getAgentId() != null) {
            SaleAgent agent = saleAgentRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new NotFoundException("Agent not found: " + request.getAgentId()));
            contract.setAgent(agent);
        }
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));
            contract.setCustomer(customer);
        }
        if (request.getPropertyValue() != null) {
            // Validate against deposit if exists
            if (contract.getDepositContract() != null &&
                request.getPropertyValue().compareTo(contract.getDepositContract().getAgreedPrice()) != 0) {
                throw new BadRequestException("Property value must match deposit contract's agreed price: " +
                        contract.getDepositContract().getAgreedPrice());
            }
            contract.setPropertyValue(request.getPropertyValue());
        }
        if (request.getAdvancePaymentAmount() != null) {
            contract.setAdvancePaymentAmount(request.getAdvancePaymentAmount());
        }
        if (request.getCommissionAmount() != null) {
            // Validate commission < property value
            BigDecimal propValue = request.getPropertyValue() != null ? request.getPropertyValue() : contract.getPropertyValue();
            if (request.getCommissionAmount().compareTo(propValue) >= 0) {
                throw new BadRequestException("Commission amount must be less than property value");
            }
            contract.setCommissionAmount(request.getCommissionAmount());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getSpecialTerms() != null) {
            contract.setSpecialTerms(request.getSpecialTerms());
        }

        PurchaseContract saved = purchaseContractRepository.save(contract);
        log.info("Updated purchase contract {}", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public void deletePurchaseContract(UUID contractId) {
        PurchaseContract contract = purchaseContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be deleted");
        }

        checkWriteAccess(contract);

        purchaseContractRepository.delete(contract);
        log.info("Deleted purchase contract {}", contractId);
    }

    // ==============================
    // PURCHASE CONTRACT TRANSITIONS
    // ==============================

    @Override
    @Transactional
    public PurchaseContractDetailResponse approvePurchaseContract(UUID contractId) {
        PurchaseContract contract = purchaseContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be approved");
        }

        checkWriteAccess(contract);

        contract.setStatus(ContractStatusEnum.WAITING_OFFICIAL);

        // If advance payment > 0, auto-create payment and notify customer
        if (contract.getAdvancePaymentAmount() != null &&
            contract.getAdvancePaymentAmount().compareTo(BigDecimal.ZERO) > 0) {

            Payment advancePayment = createPayment(
                    contract,
                    PaymentTypeEnum.ADVANCE,
                    contract.getAdvancePaymentAmount(),
                    "Advance payment for property purchase: " + contract.getProperty().getTitle()
            );

            contract.getPayments().add(advancePayment);

            // Notify customer
            notificationService.createNotification(
                    contract.getCustomer().getUser(),
                    NotificationTypeEnum.PAYMENT_DUE,
                    "Advance Payment Required",
                    String.format("Please complete your advance payment of %s VND for property: %s. Due date: %s",
                            contract.getAdvancePaymentAmount().toPlainString(),
                            contract.getProperty().getTitle(),
                            advancePayment.getDueDate()),
                    RelatedEntityTypeEnum.PAYMENT,
                    advancePayment.getId().toString(),
                    null
            );

            log.info("Created advance payment {} for purchase contract {}", advancePayment.getId(), contractId);
        }

        PurchaseContract saved = purchaseContractRepository.save(contract);
        log.info("Approved purchase contract {} -> WAITING_OFFICIAL", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseContractDetailResponse markPurchasePaperworkComplete(UUID contractId) {
        PurchaseContract contract = purchaseContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.WAITING_OFFICIAL) {
            throw new BadRequestException("Paperwork can only be marked complete when contract is in WAITING_OFFICIAL state");
        }

        checkWriteAccess(contract);

        // Check if advance payment is still pending
        boolean hasUnpaidAdvance = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.ADVANCE &&
                        p.getStatus() == PaymentStatusEnum.PENDING);

        if (hasUnpaidAdvance) {
            throw new BadRequestException("Advance payment must be completed before marking paperwork complete");
        }

        contract.setSignedAt(LocalDateTime.now());

        // Calculate remaining amount
        BigDecimal advancePaid = contract.getAdvancePaymentAmount() != null ? contract.getAdvancePaymentAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = contract.getPropertyValue().subtract(advancePaid);

        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Create final payment
            Payment finalPayment = createPayment(
                    contract,
                    PaymentTypeEnum.FULL_PAY,
                    remainingAmount,
                    "Final payment for property purchase: " + contract.getProperty().getTitle()
            );

            contract.getPayments().add(finalPayment);
            contract.setStatus(ContractStatusEnum.PENDING_PAYMENT);

            // Notify customer
            notificationService.createNotification(
                    contract.getCustomer().getUser(),
                    NotificationTypeEnum.PAYMENT_DUE,
                    "Final Payment Required",
                    String.format("Please complete your final payment of %s VND for property: %s. Due date: %s",
                            remainingAmount.toPlainString(),
                            contract.getProperty().getTitle(),
                            finalPayment.getDueDate()),
                    RelatedEntityTypeEnum.PAYMENT,
                    finalPayment.getId().toString(),
                    null
            );

            log.info("Created final payment {} for purchase contract {} -> PENDING_PAYMENT", finalPayment.getId(), contractId);
        } else {
            // No remaining payment, complete directly
            completePurchaseContract(contract);
            log.info("No remaining payment, purchase contract {} -> COMPLETED", contractId);
        }

        PurchaseContract saved = purchaseContractRepository.save(contract);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseContractDetailResponse cancelPurchaseContract(UUID contractId, CancelPurchaseContractRequest request) {
        PurchaseContract contract = purchaseContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        // Cannot cancel if already terminal
        if (contract.getStatus() == ContractStatusEnum.CANCELLED ||
            contract.getStatus() == ContractStatusEnum.COMPLETED) {
            throw new BadRequestException("Contract is already in terminal state: " + contract.getStatus());
        }

        // Cannot cancel after final payment is made
        boolean finalPaymentMade = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.FULL_PAY &&
                        (p.getStatus() == PaymentStatusEnum.SUCCESS || p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS));

        if (finalPaymentMade) {
            throw new BadRequestException("Cannot cancel after final payment is made. Contact admin to void the contract.");
        }

        UUID currentUserId = userService.getUserId();
        PropertyOwner owner = contract.getProperty().getOwner();
        UUID ownerId = owner.getId();
        UUID customerId = contract.getCustomer().getId();

        boolean isCustomer = currentUserId.equals(customerId);
        boolean isOwner = currentUserId.equals(ownerId);

        if (!isCustomer && !isOwner) {
            throw new ForbiddenException("Only customer or property owner can cancel this contract");
        }

        contract.setCancellationReason(request.getCancellationReason());
        contract.setCancelledBy(isCustomer ? RoleEnum.CUSTOMER : RoleEnum.PROPERTY_OWNER);
        contract.setCancelledAt(LocalDateTime.now());
        contract.setStatus(ContractStatusEnum.CANCELLED);

        // Refund advance payment if it was paid
        boolean advancePaid = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.ADVANCE &&
                        (p.getStatus() == PaymentStatusEnum.SUCCESS || p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS));

        if (advancePaid && contract.getAdvancePaymentAmount() != null &&
            contract.getAdvancePaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            triggerPayoutToCustomer(contract, contract.getAdvancePaymentAmount(), "Purchase cancelled - advance payment refund");
        }

        PurchaseContract saved = purchaseContractRepository.save(contract);
        log.info("Cancelled purchase contract {} by {}", contractId, contract.getCancelledBy());

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseContractDetailResponse voidPurchaseContract(UUID contractId) {
        if (!hasRole(RoleEnum.ADMIN)) {
            throw new ForbiddenException("Only admins can void contracts");
        }

        PurchaseContract contract = purchaseContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() == ContractStatusEnum.CANCELLED ||
            contract.getStatus() == ContractStatusEnum.COMPLETED) {
            throw new BadRequestException("Contract is already in terminal state: " + contract.getStatus());
        }

        contract.setStatus(ContractStatusEnum.CANCELLED);
        contract.setCancellationReason("Voided by admin");
        contract.setCancelledBy(RoleEnum.ADMIN);
        contract.setCancelledAt(LocalDateTime.now());

        PurchaseContract saved = purchaseContractRepository.save(contract);
        log.info("Voided purchase contract {} by admin", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public void onAdvancePaymentCompleted(UUID contractId) {
        PurchaseContract contract = purchaseContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.WAITING_OFFICIAL) {
            log.warn("Advance payment completed but contract {} is not in WAITING_OFFICIAL state", contractId);
            return;
        }

        // Notify agent to continue paperwork
        if (contract.getAgent() != null) {
            notificationService.createNotification(
                    contract.getAgent().getUser(),
                    NotificationTypeEnum.CONTRACT_UPDATE,
                    "Advance Payment Received",
                    String.format("Advance payment for purchase contract of property '%s' has been received. Please proceed with paperwork.",
                            contract.getProperty().getTitle()),
                    RelatedEntityTypeEnum.CONTRACT,
                    contractId.toString(),
                    null
            );
        }

        log.info("Advance payment completed for purchase contract {}, notified agent", contractId);
    }

    @Override
    @Transactional
    public void onFinalPaymentCompleted(UUID contractId) {
        PurchaseContract contract = purchaseContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Purchase contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.PENDING_PAYMENT) {
            log.warn("Final payment completed but contract {} is not in PENDING_PAYMENT state", contractId);
            return;
        }

        // Check if all payments are complete
        boolean allPaid = contract.getPayments().stream()
                .allMatch(p -> p.getStatus() == PaymentStatusEnum.SUCCESS ||
                              p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS);

        if (allPaid) {
            completePurchaseContract(contract);
            purchaseContractRepository.save(contract);
            log.info("All payments complete, purchase contract {} -> COMPLETED", contractId);
        }
    }

    // ==================
    // HELPER METHODS
    // ==================

    private void completePurchaseContract(PurchaseContract contract) {
        contract.setStatus(ContractStatusEnum.COMPLETED);

        // Payout to owner = propertyValue - commission
        BigDecimal payoutAmount = contract.getPropertyValue().subtract(contract.getCommissionAmount());
        var currentTime = LocalDate.now();
        financialUpdateService.transaction(
                contract.getProperty().getId(),
                contract.getCommissionAmount(),
                currentTime.getMonthValue(),
                currentTime.getYear()
        );
        triggerPayoutToOwner(contract, payoutAmount, "Property purchase payment");

        // Complete linked deposit contract if exists
        if (contract.getDepositContract() != null) {
            try {
                depositContractService.completeDepositContract(contract.getDepositContract().getId());
            } catch (Exception e) {
                log.error("Failed to complete linked deposit contract {}: {}",
                        contract.getDepositContract().getId(), e.getMessage());
            }
        }
    }

    private Payment createPayment(PurchaseContract contract, PaymentTypeEnum type, BigDecimal amount, String description) {
        Payment payment = Payment.builder()
                .contract(contract)
                .property(contract.getProperty())
                .payer(contract.getCustomer().getUser())
                .paymentType(type)
                .amount(amount)
                .dueDate(LocalDate.now().plusDays(DEFAULT_PAYMENT_DUE_DAYS))
                .status(PaymentStatusEnum.PENDING)
                .paymentMethod(PAYOS_METHOD)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Call payment gateway
        CreatePaymentSessionRequest gatewayRequest = CreatePaymentSessionRequest.builder()
                .amount(amount)
                .currency(CURRENCY_VND)
                .description(description)
                .metadata(Map.of(
                        "paymentType", type.getValue(),
                        "contractId", contract.getId().toString(),
                        "paymentId", savedPayment.getId().toString()
                ))
                .build();

        CreatePaymentSessionResponse gatewayResponse = paymentGatewayService.createPaymentSession(
                gatewayRequest,
                savedPayment.getId().toString()
        );

        savedPayment.setPaywayPaymentId(gatewayResponse.getId());
        return paymentRepository.save(savedPayment);
    }

    private void checkReadAccess(PurchaseContract contract) {
        boolean isAdmin = hasRole(RoleEnum.ADMIN);
        if (isAdmin) return;

        UUID currentUserId = userService.getUserId();
        UUID agentId = contract.getAgent() != null ? contract.getAgent().getId() : null;
        UUID customerId = contract.getCustomer().getId();
        UUID ownerId = contract.getProperty().getOwner().getId();

        boolean isAgent = hasRole(RoleEnum.SALESAGENT) && currentUserId.equals(agentId);
        boolean isCustomer = currentUserId.equals(customerId);
        boolean isOwner = currentUserId.equals(ownerId);

        if (!isAgent && !isCustomer && !isOwner) {
            throw new ForbiddenException("You don't have access to this contract");
        }
    }

    private void checkWriteAccess(PurchaseContract contract) {
        boolean isAdmin = hasRole(RoleEnum.ADMIN);
        if (isAdmin) return;

        boolean isAgent = hasRole(RoleEnum.SALESAGENT);
        if (!isAgent) {
            throw new ForbiddenException("Only admins and sales agents can modify contracts");
        }

        UUID currentUserId = userService.getUserId();
        UUID agentId = contract.getAgent() != null ? contract.getAgent().getId() : null;

        if (!currentUserId.equals(agentId)) {
            throw new ForbiddenException("You can only modify contracts you are assigned to");
        }
    }

    private boolean hasRole(RoleEnum role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.getValue()));
    }

    private void triggerPayoutToOwner(PurchaseContract contract, BigDecimal amount, String description) {
        PropertyOwner owner = contract.getProperty().getOwner();
        User ownerUser = owner.getUser();

        // ensure owner has bank details
        if (ownerUser.getBankAccountNumber() == null || ownerUser.getBankAccountName() == null || ownerUser.getBankBin() == null) {
            log.error("Cannot trigger payout to owner {} for contract {}: missing bank details",
                    owner.getId(), contract.getId());
            // send notification to admin to manually transfer
            notificationService.createNotification(
                    // TODO: put the admin getter somewhere portable and reusable
                    userService.findByEmail("admin@example.com"),
                    NotificationTypeEnum.SYSTEM_ALERT,
                    "Payout Failed - Missing Bank Details",
                    String.format("Cannot process payout of %s VND to property owner '%s' (ID: %s) for purchase contract %s due to missing bank details. Please process manually.",
                            amount.toPlainString(),
                            ownerUser.getFullName(),
                            ownerUser.getId(),
                            contract.getId()),
                    RelatedEntityTypeEnum.CONTRACT,
                    contract.getId().toString(),
                    null
            );
            return;
        }

        // TODO: Add payout entity tracking when entity is added
        CreatePayoutSessionRequest payoutRequest = CreatePayoutSessionRequest.builder()
                .amount(amount)
                .currency(CURRENCY_VND)
                .accountNumber(ownerUser.getBankAccountNumber())
                .accountHolderName(ownerUser.getBankAccountName())
                .swiftCode(ownerUser.getBankBin())
                .description(description + " - Contract: " + contract.getId())
                .metadata(Map.of(
                        "contractId", contract.getId().toString(),
                        "payoutType", "PURCHASE_TO_OWNER"
                ))
                .build();

        paymentGatewayService.createPayoutSession(payoutRequest,
                "payout-owner-" + contract.getId() + "-" + System.currentTimeMillis());

        log.info("Triggered payout to owner {} for purchase contract {}: {} VND",
                owner.getId(), contract.getId(), amount);
    }

    private void triggerPayoutToCustomer(PurchaseContract contract, BigDecimal amount, String description) {
        User customerUser = contract.getCustomer().getUser();

        // TODO: Add payout entity tracking when entity is added
        CreatePayoutSessionRequest payoutRequest = CreatePayoutSessionRequest.builder()
                .amount(amount)
                .currency(CURRENCY_VND)
                .accountNumber(customerUser.getBankAccountNumber())
                .accountHolderName(customerUser.getBankAccountName())
                .swiftCode(customerUser.getBankBin())
                .description(description + " - Contract: " + contract.getId())
                .metadata(Map.of(
                        "contractId", contract.getId().toString(),
                        "payoutType", "PURCHASE_REFUND_TO_CUSTOMER"
                ))
                .build();

        paymentGatewayService.createPayoutSession(payoutRequest,
                "payout-customer-" + contract.getId() + "-" + System.currentTimeMillis());

        log.info("Triggered payout to customer {} for purchase contract {}: {} VND",
                contract.getCustomer().getId(), contract.getId(), amount);
    }

    private Specification<PurchaseContract> buildPurchaseContractSpecification(
            List<ContractStatusEnum> statuses,
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            String search,
            boolean isAdmin,
            boolean isAgent
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isAgent && !isAdmin) {
                UUID currentUserId = userService.getUserId();
                predicates.add(cb.equal(root.get("agent").get("id"), currentUserId));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (agentId != null) {
                predicates.add(cb.equal(root.get("agent").get("id"), agentId));
            }
            if (propertyId != null) {
                predicates.add(cb.equal(root.get("property").get("id"), propertyId));
            }
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("property").get("owner").get("id"), ownerId));
            }
            if (startDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
            }
            if (startDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), startDateTo));
            }
            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                Predicate contractNumberMatch = cb.like(cb.lower(root.get("contractNumber")), likePattern);
                Predicate propertyTitleMatch = cb.like(cb.lower(root.get("property").get("title")), likePattern);
                predicates.add(cb.or(contractNumberMatch, propertyTitleMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PurchaseContractDetailResponse mapToDetailResponse(PurchaseContract contract) {
        Property property = contract.getProperty();
        Customer customer = contract.getCustomer();
        SaleAgent agent = contract.getAgent();
        PropertyOwner owner = property.getOwner();
        DepositContract deposit = contract.getDepositContract();

        List<PaymentSummary> paymentSummaries = contract.getPayments().stream()
                .map(p -> PaymentSummary.builder()
                        .id(p.getId())
                        .paymentType(p.getPaymentType().getValue())
                        .amount(p.getAmount())
                        .dueDate(p.getDueDate())
                        .paidTime(p.getPaidTime())
                        .status(p.getStatus() != null ? p.getStatus().getValue() : null)
                        .checkoutUrl(getCheckoutUrl(p))
                        .build())
                .toList();

        return PurchaseContractDetailResponse.builder()
                .id(contract.getId())
                .status(contract.getStatus())
                .contractNumber(contract.getContractNumber())
                .propertyValue(contract.getPropertyValue())
                .advancePaymentAmount(contract.getAdvancePaymentAmount())
                .commissionAmount(contract.getCommissionAmount())
                .startDate(contract.getStartDate())
                .signedAt(contract.getSignedAt())
                .specialTerms(contract.getSpecialTerms())
                .cancellationReason(contract.getCancellationReason())
                .cancelledBy(contract.getCancelledBy())
                .cancelledAt(contract.getCancelledAt())
                .property(PropertySummary.builder()
                        .id(property.getId())
                        .title(property.getTitle())
                        .fullAddress(property.getFullAddress())
                        .priceAmount(property.getPriceAmount())
                        .build())
                .customer(mapUserSummary(customer.getUser()))
                .owner(mapUserSummary(owner.getUser()))
                .agent(agent != null ? mapUserSummary(agent.getUser()) : null)
                .depositContractId(deposit != null ? deposit.getId() : null)
                .depositContractStatus(deposit != null ? deposit.getStatus() : null)
                .payments(paymentSummaries)
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    private PurchaseContractListItem mapToListItem(PurchaseContract contract) {
        Property property = contract.getProperty();
        Customer customer = contract.getCustomer();
        SaleAgent agent = contract.getAgent();

        String customerName = customer.getUser().getFirstName() + " " + customer.getUser().getLastName();
        String agentName = agent != null
                ? agent.getUser().getFirstName() + " " + agent.getUser().getLastName()
                : null;

        return PurchaseContractListItem.builder()
                .id(contract.getId())
                .status(contract.getStatus())
                .contractNumber(contract.getContractNumber())
                .propertyValue(contract.getPropertyValue())
                .advancePaymentAmount(contract.getAdvancePaymentAmount())
                .commissionAmount(contract.getCommissionAmount())
                .startDate(contract.getStartDate())
                .propertyId(property.getId())
                .propertyTitle(property.getTitle())
                .customerId(customer.getId())
                .customerName(customerName)
                .agentId(agent != null ? agent.getId() : null)
                .agentName(agentName)
                .hasDepositContract(contract.getDepositContract() != null)
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    private UserSummary mapUserSummary(User user) {
        if (user == null) return null;
        return UserSummary.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .build();
    }

    private String getCheckoutUrl(Payment payment) {
        if (payment.getPaywayPaymentId() == null) return null;
        try {
            CreatePaymentSessionResponse session = paymentGatewayService.getPaymentSession(payment.getPaywayPaymentId());
            return session != null ? session.getCheckoutUrl() : null;
        } catch (Exception e) {
            log.warn("Failed to get checkout URL for payment {}: {}", payment.getId(), e.getMessage());
            return null;
        }
    }
}

