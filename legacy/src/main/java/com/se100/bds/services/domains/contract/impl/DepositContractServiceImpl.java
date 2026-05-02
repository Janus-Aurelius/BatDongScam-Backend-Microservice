package com.se100.bds.services.domains.contract.impl;

import com.se100.bds.dtos.requests.contract.CancelDepositContractRequest;
import com.se100.bds.dtos.requests.contract.CreateDepositContractRequest;
import com.se100.bds.dtos.requests.contract.UpdateDepositContractRequest;
import com.se100.bds.dtos.responses.contract.DepositContractDetailResponse;
import com.se100.bds.dtos.responses.contract.DepositContractDetailResponse.PaymentSummary;
import com.se100.bds.dtos.responses.contract.DepositContractDetailResponse.PropertySummary;
import com.se100.bds.dtos.responses.contract.DepositContractDetailResponse.UserSummary;
import com.se100.bds.dtos.responses.contract.DepositContractListItem;
import com.se100.bds.exceptions.BadRequestException;
import com.se100.bds.exceptions.ForbiddenException;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.contract.DepositContractRepository;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.services.domains.contract.DepositContractService;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.payment.PaymentGatewayService;
import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.services.payment.dto.CreatePayoutSessionRequest;
import com.se100.bds.utils.Constants.ContractStatusEnum;
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
public class DepositContractServiceImpl implements DepositContractService {

    private final DepositContractRepository depositContractRepository;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final UserService userService;
    private final PaymentGatewayService paymentGatewayService;
    private final NotificationService notificationService;

    private static final int DEFAULT_PAYMENT_DUE_DAYS = 7;
    private static final String CURRENCY_VND = "VND";
    private static final String PAYOS_METHOD = "PAYOS";

    // =====================
    // DEPOSIT CONTRACT CRUD
    // =====================

    @Override
    @Transactional
    public DepositContractDetailResponse createDepositContract(CreateDepositContractRequest request) {
        // Check if user is admin or agent
        boolean isAdmin = hasRole(RoleEnum.ADMIN);
        boolean isAgent = hasRole(RoleEnum.SALESAGENT);

        if (!isAdmin && !isAgent) {
            throw new ForbiddenException("Only admins and sales agents can create deposit contracts");
        }

        // Validate property exists
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NotFoundException("Property not found: " + request.getPropertyId()));

        // Check no non-DRAFT deposit contract exists for this property
        if (depositContractRepository.existsNonDraftDepositContractForProperty(
                request.getPropertyId(), ContractStatusEnum.DRAFT)) {
            throw new BadRequestException("A non-draft deposit contract already exists for this property. " +
                    "Only one active deposit contract is allowed per property.");
        }

        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));

        // Determine agent
        SaleAgent agent;
        if (isAdmin) {
            if (request.getAgentId() == null) {
                throw new BadRequestException("Agent ID is required when admin creates contract");
            }
            agent = saleAgentRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new NotFoundException("Agent not found: " + request.getAgentId()));
        } else {
            // Current user is agent
            UUID currentUserId = userService.getUserId();
            agent = saleAgentRepository.findById(currentUserId)
                    .orElseThrow(() -> new NotFoundException("Current agent not found"));
        }

        // Create the deposit contract
        DepositContract contract = new DepositContract();
        contract.setProperty(property);
        contract.setCustomer(customer);
        contract.setAgent(agent);
        contract.setStatus(ContractStatusEnum.DRAFT);
        contract.setMainContractType(request.getMainContractType());
        contract.setDepositAmount(request.getDepositAmount());
        contract.setAgreedPrice(request.getAgreedPrice());
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(request.getEndDate());
        contract.setSpecialTerms(request.getSpecialTerms());
        contract.setCancellationPenalty(request.getCancellationPenalty());

        DepositContract saved = depositContractRepository.save(contract);
        log.info("Created deposit contract {} for property {}", saved.getId(), property.getId());

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositContractDetailResponse getDepositContractById(UUID contractId) {
        DepositContract contract = depositContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        // Check access
        checkReadAccess(contract);

        return mapToDetailResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepositContractListItem> getDepositContracts(
            Pageable pageable,
            List<ContractStatusEnum> statuses,
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
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

        Specification<DepositContract> spec = buildDepositContractSpecification(
                statuses, customerId, agentId, propertyId, ownerId,
                startDateFrom, startDateTo, endDateFrom, endDateTo, search,
                isAdmin, isAgent
        );

        return depositContractRepository.findAll(spec, pageable).map(this::mapToListItem);
    }

    @Override
    @Transactional
    public DepositContractDetailResponse updateDepositContract(UUID contractId, UpdateDepositContractRequest request) {
        DepositContract contract = depositContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        // Only DRAFT can be updated
        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be updated");
        }

        // Check write access
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
        if (request.getMainContractType() != null) {
            contract.setMainContractType(request.getMainContractType());
        }
        if (request.getDepositAmount() != null) {
            contract.setDepositAmount(request.getDepositAmount());
        }
        if (request.getAgreedPrice() != null) {
            contract.setAgreedPrice(request.getAgreedPrice());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            contract.setEndDate(request.getEndDate());
        }
        if (request.getSpecialTerms() != null) {
            contract.setSpecialTerms(request.getSpecialTerms());
        }
        if (request.getCancellationPenalty() != null) {
            contract.setCancellationPenalty(request.getCancellationPenalty());
        }

        DepositContract saved = depositContractRepository.save(contract);
        log.info("Updated deposit contract {}", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDepositContract(UUID contractId) {
        DepositContract contract = depositContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        // Only DRAFT can be deleted
        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be deleted");
        }

        // Check write access
        checkWriteAccess(contract);

        depositContractRepository.delete(contract);
        log.info("Deleted deposit contract {}", contractId);
    }

    // ============================
    // DEPOSIT CONTRACT TRANSITIONS
    // ============================

    @Override
    @Transactional
    public DepositContractDetailResponse approveDepositContract(UUID contractId) {
        DepositContract contract = depositContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be approved");
        }

        checkWriteAccess(contract);

        contract.setStatus(ContractStatusEnum.WAITING_OFFICIAL);
        DepositContract saved = depositContractRepository.save(contract);
        log.info("Approved deposit contract {} -> WAITING_OFFICIAL", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public DepositContractDetailResponse createDepositPayment(UUID contractId) {
        DepositContract contract = depositContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.WAITING_OFFICIAL) {
            throw new BadRequestException("Payments can only be created when contract is in WAITING_OFFICIAL state");
        }

        checkWriteAccess(contract);

        // Check if payment already exists
        boolean hasDepositPayment = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.DEPOSIT);
        if (hasDepositPayment) {
            throw new BadRequestException("Deposit payment already exists for this contract");
        }

        // Create payment entity
        Payment payment = Payment.builder()
                .contract(contract)
                .property(contract.getProperty())
                .payer(contract.getCustomer().getUser())
                .paymentType(PaymentTypeEnum.DEPOSIT)
                .amount(contract.getDepositAmount())
                .dueDate(LocalDate.now().plusDays(DEFAULT_PAYMENT_DUE_DAYS))
                .status(PaymentStatusEnum.PENDING)
                .paymentMethod(PAYOS_METHOD)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Call payment gateway
        CreatePaymentSessionRequest gatewayRequest = CreatePaymentSessionRequest.builder()
                .amount(contract.getDepositAmount())
                .currency(CURRENCY_VND)
                .description("Deposit payment for property: " + contract.getProperty().getTitle())
                .metadata(Map.of(
                        "paymentType", PaymentTypeEnum.DEPOSIT.getValue(),
                        "contractId", contractId.toString(),
                        "paymentId", savedPayment.getId().toString()
                ))
                .build();

        CreatePaymentSessionResponse gatewayResponse = paymentGatewayService.createPaymentSession(
                gatewayRequest,
                savedPayment.getId().toString() // idempotency key
        );

        savedPayment.setPaywayPaymentId(gatewayResponse.getId());
        paymentRepository.save(savedPayment);

        // Send notification to customer
        User customerUser = contract.getCustomer().getUser();
        notificationService.createNotification(
                customerUser,
                NotificationTypeEnum.PAYMENT_DUE,
                "Deposit Payment Required",
                String.format("Please complete your deposit payment of %s VND for property: %s. Due date: %s",
                        contract.getDepositAmount().toPlainString(),
                        contract.getProperty().getTitle(),
                        savedPayment.getDueDate()),
                RelatedEntityTypeEnum.PAYMENT,
                savedPayment.getId().toString(),
                null
        );

        log.info("Created deposit payment {} for contract {}", savedPayment.getId(), contractId);

        // Refresh contract to include the new payment
        contract.getPayments().add(savedPayment);
        return mapToDetailResponse(contract);
    }

    @Override
    @Transactional
    public DepositContractDetailResponse markDepositPaperworkComplete(UUID contractId) {
        DepositContract contract = depositContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.WAITING_OFFICIAL) {
            throw new BadRequestException("Paperwork can only be marked complete when contract is in WAITING_OFFICIAL state");
        }

        checkWriteAccess(contract);

        contract.setSignedAt(LocalDateTime.now());

        // Check if there are pending payments
        boolean hasPendingPayments = contract.getPayments().stream()
                .anyMatch(p -> p.getStatus() == PaymentStatusEnum.PENDING);

        if (hasPendingPayments) {
            contract.setStatus(ContractStatusEnum.PENDING_PAYMENT);
            log.info("Marked deposit contract {} paperwork complete -> PENDING_PAYMENT", contractId);
        } else {
            // Check if we need to auto-create payment
            boolean hasAnyPayment = !contract.getPayments().isEmpty();
            if (!hasAnyPayment) {
                // Auto-create payment
                Payment payment = Payment.builder()
                        .contract(contract)
                        .property(contract.getProperty())
                        .payer(contract.getCustomer().getUser())
                        .paymentType(PaymentTypeEnum.DEPOSIT)
                        .amount(contract.getDepositAmount())
                        .dueDate(LocalDate.now().plusDays(DEFAULT_PAYMENT_DUE_DAYS))
                        .status(PaymentStatusEnum.PENDING)
                        .paymentMethod(PAYOS_METHOD)
                        .build();

                Payment savedPayment = paymentRepository.save(payment);

                // Call payment gateway
                CreatePaymentSessionRequest gatewayRequest = CreatePaymentSessionRequest.builder()
                        .amount(contract.getDepositAmount())
                        .currency(CURRENCY_VND)
                        .description("Deposit payment for property: " + contract.getProperty().getTitle())
                        .metadata(Map.of(
                                "paymentType", PaymentTypeEnum.DEPOSIT.getValue(),
                                "contractId", contractId.toString(),
                                "paymentId", savedPayment.getId().toString()
                        ))
                        .build();

                CreatePaymentSessionResponse gatewayResponse = paymentGatewayService.createPaymentSession(
                        gatewayRequest,
                        savedPayment.getId().toString()
                );

                savedPayment.setPaywayPaymentId(gatewayResponse.getId());
                paymentRepository.save(savedPayment);

                // Send notification
                User customerUser = contract.getCustomer().getUser();
                notificationService.createNotification(
                        customerUser,
                        NotificationTypeEnum.PAYMENT_DUE,
                        "Deposit Payment Required",
                        String.format("Please complete your deposit payment of %s VND for property: %s. Due date: %s",
                                contract.getDepositAmount().toPlainString(),
                                contract.getProperty().getTitle(),
                                savedPayment.getDueDate()),
                        RelatedEntityTypeEnum.PAYMENT,
                        savedPayment.getId().toString(),
                        null
                );

                contract.getPayments().add(savedPayment);
                contract.setStatus(ContractStatusEnum.PENDING_PAYMENT);
                log.info("Auto-created payment and marked deposit contract {} paperwork complete -> PENDING_PAYMENT", contractId);
            } else {
                // All payments are complete
                contract.setStatus(ContractStatusEnum.ACTIVE);
                log.info("Marked deposit contract {} paperwork complete -> ACTIVE", contractId);
            }
        }

        DepositContract saved = depositContractRepository.save(contract);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public DepositContractDetailResponse cancelDepositContract(UUID contractId, CancelDepositContractRequest request) {
        DepositContract contract = depositContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        // Can only cancel non-terminal contracts
        if (contract.getStatus() == ContractStatusEnum.CANCELLED ||
            contract.getStatus() == ContractStatusEnum.COMPLETED) {
            throw new BadRequestException("Contract is already in terminal state: " + contract.getStatus());
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

        // Save original status before changing
        ContractStatusEnum originalStatus = contract.getStatus();

        contract.setCancellationReason(request.getCancellationReason());
        contract.setCancelledBy(isCustomer ? RoleEnum.CUSTOMER : RoleEnum.PROPERTY_OWNER);
        contract.setCancelledAt(LocalDateTime.now());
        contract.setStatus(ContractStatusEnum.CANCELLED);

        // Handle money transfer based on who cancelled (only if contract was active or pending payment)
        if (originalStatus == ContractStatusEnum.ACTIVE ||
            originalStatus == ContractStatusEnum.PENDING_PAYMENT) {

            // Check if deposit was paid
            boolean depositPaid = contract.getPayments().stream()
                    .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.DEPOSIT &&
                            (p.getStatus() == PaymentStatusEnum.SUCCESS || p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS));

            if (depositPaid) {
                if (isCustomer) {
                    // Customer cancels: deposit goes to owner
                    triggerPayoutToOwner(contract, contract.getDepositAmount(), "Customer cancelled - deposit forfeited");
                } else {
                    // Owner cancels: deposit returns to customer
                    triggerPayoutToCustomer(contract, contract.getDepositAmount(), "Owner cancelled - deposit returned");

                    // Owner pays penalty (if not the same as deposit, otherwise it's just the deposit returned)
                    BigDecimal penalty = contract.getCancellationPenalty() != null
                            ? contract.getCancellationPenalty()
                            : contract.getDepositAmount();

                    // TODO: Create payout entity to track this when entity is added
                    // TODO: revise this part. how do we mediate between owner and customer for penalty payment?
                    // For now, trigger payout to customer for the penalty amount from owner
                    triggerPayoutToCustomer(contract, penalty, "Owner cancellation penalty");
                }
            }
        }

        DepositContract saved = depositContractRepository.save(contract);
        log.info("Cancelled deposit contract {} by {}", contractId, contract.getCancelledBy());

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public DepositContractDetailResponse voidDepositContract(UUID contractId) {
        // Only admins can void
        if (!hasRole(RoleEnum.ADMIN)) {
            throw new ForbiddenException("Only admins can void contracts");
        }

        DepositContract contract = depositContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        if (contract.getStatus() == ContractStatusEnum.CANCELLED ||
            contract.getStatus() == ContractStatusEnum.COMPLETED) {
            throw new BadRequestException("Contract is already in terminal state: " + contract.getStatus());
        }

        contract.setStatus(ContractStatusEnum.CANCELLED);
        contract.setCancellationReason("Voided by admin");
        contract.setCancelledBy(RoleEnum.ADMIN);
        contract.setCancelledAt(LocalDateTime.now());

        DepositContract saved = depositContractRepository.save(contract);
        log.info("Voided deposit contract {} by admin", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public void onDepositPaymentCompleted(UUID contractId) {
        DepositContract contract = depositContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.PENDING_PAYMENT) {
            log.warn("Payment completed but contract {} is not in PENDING_PAYMENT state", contractId);
            return;
        }

        // Check if all payments are complete
        boolean allPaid = contract.getPayments().stream()
                .allMatch(p -> p.getStatus() == PaymentStatusEnum.SUCCESS ||
                              p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS);

        if (allPaid) {
            contract.setStatus(ContractStatusEnum.ACTIVE);
            depositContractRepository.save(contract);
            log.info("All payments complete, deposit contract {} -> ACTIVE", contractId);
        }
    }

    @Override
    @Transactional
    public void completeDepositContract(UUID contractId) {
        DepositContract contract = depositContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.ACTIVE) {
            throw new BadRequestException("Only ACTIVE contracts can be completed");
        }

        // Refund deposit to customer (since main contract is now active, deposit is returned)
        boolean depositPaid = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.DEPOSIT &&
                        (p.getStatus() == PaymentStatusEnum.SUCCESS || p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS));

        if (depositPaid) {
            triggerPayoutToCustomer(contract, contract.getDepositAmount(), "Deposit refund - main contract completed");
        }

        contract.setStatus(ContractStatusEnum.COMPLETED);
        depositContractRepository.save(contract);
        log.info("Completed deposit contract {} and refunded deposit to customer", contractId);
    }

    // ==================
    // HELPER METHODS
    // ==================

    private void checkReadAccess(DepositContract contract) {
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

    private void checkWriteAccess(DepositContract contract) {
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

    private void triggerPayoutToOwner(DepositContract contract, BigDecimal amount, String description) {
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
                    String.format("Cannot process payout of %s VND to property owner '%s' (ID: %s) for deposit contract %s due to missing bank details. Please process manually.",
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
                .swiftCode(ownerUser.getBankBin()) // Using bank BIN as identifier
                .description(description + " - Contract: " + contract.getId())
                .metadata(Map.of(
                        "contractId", contract.getId().toString(),
                        "payoutType", "DEPOSIT_TO_OWNER"
                ))
                .build();

        paymentGatewayService.createPayoutSession(payoutRequest,
                "payout-owner-" + contract.getId() + "-" + System.currentTimeMillis());

        log.info("Triggered payout to owner {} for contract {}: {} VND",
                owner.getId(), contract.getId(), amount);
    }

    private void triggerPayoutToCustomer(DepositContract contract, BigDecimal amount, String description) {
        User customerUser = contract.getCustomer().getUser();

        // ensure owner has bank details
        if (customerUser.getBankAccountNumber() == null || customerUser.getBankAccountName() == null || customerUser.getBankBin() == null) {
            log.error("Cannot trigger payout to owner {} for contract {}: missing bank details",
                    customerUser.getId(), contract.getId());
            // send notification to admin to manually transfer
            notificationService.createNotification(
                    // TODO: put the admin getter somewhere portable and reusable
                    userService.findByEmail("admin@example.com"),
                    NotificationTypeEnum.SYSTEM_ALERT,
                    "Payout Failed - Missing Bank Details",
                    String.format("Cannot process payout of %s VND to customer '%s' (ID: %s) for deposit contract %s due to missing bank details. Please process manually.",
                            amount.toPlainString(),
                            customerUser.getFullName(),
                            customerUser.getId(),
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
                .accountNumber(customerUser.getBankAccountNumber())
                .accountHolderName(customerUser.getBankAccountName())
                .swiftCode(customerUser.getBankBin()) // Using bank BIN as identifier
                .description(description + " - Contract: " + contract.getId())
                .metadata(Map.of(
                        "contractId", contract.getId().toString(),
                        "payoutType", "DEPOSIT_TO_CUSTOMER"
                ))
                .build();

        paymentGatewayService.createPayoutSession(payoutRequest,
                "payout-customer-" + contract.getId() + "-" + System.currentTimeMillis());

        log.info("Triggered payout to customer {} for contract {}: {} VND",
                contract.getCustomer().getId(), contract.getId(), amount);
    }

    private Specification<DepositContract> buildDepositContractSpecification(
            List<ContractStatusEnum> statuses,
            UUID customerId,
            UUID agentId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            String search,
            boolean isAdmin,
            boolean isAgent
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // If agent (not admin), only show their assigned contracts
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
            if (endDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), endDateFrom));
            }
            if (endDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDateTo));
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

    private DepositContractDetailResponse mapToDetailResponse(DepositContract contract) {
        Property property = contract.getProperty();
        Customer customer = contract.getCustomer();
        SaleAgent agent = contract.getAgent();
        PropertyOwner owner = property.getOwner();

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

        return DepositContractDetailResponse.builder()
                .id(contract.getId())
                .status(contract.getStatus())
                .contractNumber(contract.getContractNumber())
                .mainContractType(contract.getMainContractType())
                .depositAmount(contract.getDepositAmount())
                .agreedPrice(contract.getAgreedPrice())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .signedAt(contract.getSignedAt())
                .specialTerms(contract.getSpecialTerms())
                .cancellationPenalty(contract.getCancellationPenalty())
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
                .payments(paymentSummaries)
                .linkedToMainContract(contract.isLinkedToMainContract())
                .linkedRentalContractId(contract.getRentalContract() != null ? contract.getRentalContract().getId() : null)
                .linkedPurchaseContractId(contract.getPurchaseContract() != null ? contract.getPurchaseContract().getId() : null)
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    private DepositContractListItem mapToListItem(DepositContract contract) {
        Property property = contract.getProperty();
        Customer customer = contract.getCustomer();
        SaleAgent agent = contract.getAgent();

        String customerName = customer.getUser().getFirstName() + " " + customer.getUser().getLastName();
        String agentName = agent != null
                ? agent.getUser().getFirstName() + " " + agent.getUser().getLastName()
                : null;

        return DepositContractListItem.builder()
                .id(contract.getId())
                .status(contract.getStatus())
                .contractNumber(contract.getContractNumber())
                .mainContractType(contract.getMainContractType())
                .depositAmount(contract.getDepositAmount())
                .agreedPrice(contract.getAgreedPrice())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .propertyId(property.getId())
                .propertyTitle(property.getTitle())
                .customerId(customer.getId())
                .customerName(customerName)
                .agentId(agent != null ? agent.getId() : null)
                .agentName(agentName)
                .linkedToMainContract(contract.isLinkedToMainContract())
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

