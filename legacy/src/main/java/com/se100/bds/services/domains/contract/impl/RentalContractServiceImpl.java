package com.se100.bds.services.domains.contract.impl;

import com.se100.bds.dtos.requests.contract.CreateRentalContractRequest;
import com.se100.bds.dtos.requests.contract.SecurityDepositDecisionRequest;
import com.se100.bds.dtos.requests.contract.UpdateRentalContractRequest;
import com.se100.bds.dtos.responses.contract.RentalContractDetailResponse;
import com.se100.bds.dtos.responses.contract.RentalContractDetailResponse.PaymentSummary;
import com.se100.bds.dtos.responses.contract.RentalContractDetailResponse.PropertySummary;
import com.se100.bds.dtos.responses.contract.RentalContractDetailResponse.UserSummary;
import com.se100.bds.dtos.responses.contract.RentalContractListItem;
import com.se100.bds.exceptions.BadRequestException;
import com.se100.bds.exceptions.ForbiddenException;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.entities.contract.RentalContract;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.contract.DepositContractRepository;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.repositories.domains.contract.RentalContractRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.services.domains.contract.DepositContractService;
import com.se100.bds.services.domains.contract.RentalContractService;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.domains.report.FinancialUpdateService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.payment.PaymentGatewayService;
import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.services.payment.dto.CreatePayoutSessionRequest;
import com.se100.bds.utils.Constants;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import com.se100.bds.utils.Constants.MainContractTypeEnum;
import com.se100.bds.utils.Constants.NotificationTypeEnum;
import com.se100.bds.utils.Constants.PaymentStatusEnum;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import com.se100.bds.utils.Constants.RelatedEntityTypeEnum;
import com.se100.bds.utils.Constants.RoleEnum;
import com.se100.bds.utils.Constants.SecurityDepositStatusEnum;
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
public class RentalContractServiceImpl implements RentalContractService {

    private final RentalContractRepository rentalContractRepository;
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
    // RENTAL CONTRACT CRUD
    // ========================

    @Override
    @Transactional
    public RentalContractDetailResponse createRentalContract(CreateRentalContractRequest request) {
        boolean isAdmin = hasRole(RoleEnum.ADMIN);
        boolean isAgent = hasRole(RoleEnum.SALESAGENT);

        if (!isAdmin && !isAgent) {
            throw new ForbiddenException("Only admins and sales agents can create rental contracts");
        }

        // Validate property exists
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NotFoundException("Property not found: " + request.getPropertyId()));

        // Check no non-DRAFT rental contract exists for this property
        if (rentalContractRepository.existsNonDraftRentalContractForProperty(
                request.getPropertyId(), ContractStatusEnum.DRAFT)) {
            throw new BadRequestException("A non-draft rental contract already exists for this property. " +
                    "Only one active rental contract is allowed per property.");
        }

        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));

        // Validate commission < monthly rent
        if (request.getCommissionAmount().compareTo(request.getMonthlyRentAmount()) >= 0) {
            throw new BadRequestException("Commission amount must be less than monthly rent amount");
        }

        // Validate deposit contract if provided
        DepositContract depositContract = null;
        if (request.getDepositContractId() != null) {
            depositContract = depositContractRepository.findById(request.getDepositContractId())
                    .orElseThrow(() -> new NotFoundException("Deposit contract not found: " + request.getDepositContractId()));

            validateDepositContract(request, depositContract);
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

        // Calculate end date
        LocalDate endDate = request.getStartDate().plusMonths(request.getMonthCount());

        // Create the rental contract
        RentalContract contract = new RentalContract();
        contract.setProperty(property);
        contract.setCustomer(customer);
        contract.setAgent(agent);
        contract.setStatus(ContractStatusEnum.DRAFT);
        contract.setDepositContract(depositContract);
        contract.setMonthCount(request.getMonthCount());
        contract.setMonthlyRentAmount(request.getMonthlyRentAmount());
        contract.setCommissionAmount(request.getCommissionAmount());
        contract.setSecurityDepositAmount(request.getSecurityDepositAmount() != null ? request.getSecurityDepositAmount() : BigDecimal.ZERO);
        contract.setSecurityDepositStatus(SecurityDepositStatusEnum.NOT_PAID);
        contract.setLatePaymentPenaltyRate(request.getLatePaymentPenaltyRate());
        contract.setAccumulatedUnpaidPenalty(BigDecimal.ZERO);
        contract.setUnpaidMonthsCount(0);
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(endDate);
        contract.setSpecialTerms(request.getSpecialTerms());

        RentalContract saved = rentalContractRepository.save(contract);
        log.info("Created rental contract {} for property {}", saved.getId(), property.getId());

        return mapToDetailResponse(saved);
    }

    private static void validateDepositContract(CreateRentalContractRequest request, DepositContract depositContract) {
        if (depositContract.getStatus() != ContractStatusEnum.ACTIVE) {
            throw new BadRequestException("Deposit contract must be in ACTIVE status");
        }

        if (depositContract.getEndDate() != null && depositContract.getEndDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Deposit contract has expired");
        }

        if (depositContract.getMainContractType() != MainContractTypeEnum.RENTAL) {
            throw new BadRequestException("Deposit contract must be for RENTAL type");
        }

        if (!depositContract.getProperty().getId().equals(request.getPropertyId())) {
            throw new BadRequestException("Deposit contract property does not match");
        }

        if (!depositContract.getCustomer().getId().equals(request.getCustomerId())) {
            throw new BadRequestException("Deposit contract customer does not match");
        }

        if (request.getMonthlyRentAmount().compareTo(depositContract.getAgreedPrice()) != 0) {
            throw new BadRequestException("Monthly rent amount must match deposit contract's agreed price: " +
                    depositContract.getAgreedPrice());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RentalContractDetailResponse getRentalContractById(UUID contractId) {
        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        checkReadAccess(contract);

        return mapToDetailResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalContractListItem> getRentalContracts(
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

        if (!isAdmin && !isAgent) {
            if (isCustomer && customerId != null && customerId.equals(currentUserId)) {
                // Customer querying their own contracts - allowed
            } else if (isOwner && ownerId != null && ownerId.equals(currentUserId)) {
                // Owner querying their property contracts - allowed
            } else {
                throw new ForbiddenException("Only admins and sales agents can query rental contracts");
            }
        }

        Specification<RentalContract> spec = buildRentalContractSpecification(
                statuses, customerId, agentId, propertyId, ownerId,
                startDateFrom, startDateTo, endDateFrom, endDateTo, search, isAdmin, isAgent
        );

        return rentalContractRepository.findAll(spec, pageable).map(this::mapToListItem);
    }

    @SuppressWarnings("D")
    @Override
    @Transactional
    public RentalContractDetailResponse updateRentalContract(UUID contractId, UpdateRentalContractRequest request) {
        RentalContract contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be updated");
        }

        checkWriteAccess(contract);

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
        if (request.getMonthCount() != null) {
            contract.setMonthCount(request.getMonthCount());
            if (contract.getStartDate() != null) {
                contract.setEndDate(contract.getStartDate().plusMonths(request.getMonthCount()));
            }
        }
        if (request.getMonthlyRentAmount() != null) {
            if (contract.getDepositContract() != null &&
                request.getMonthlyRentAmount().compareTo(contract.getDepositContract().getAgreedPrice()) != 0) {
                throw new BadRequestException("Monthly rent amount must match deposit contract's agreed price: " +
                        contract.getDepositContract().getAgreedPrice());
            }
            contract.setMonthlyRentAmount(request.getMonthlyRentAmount());
        }
        if (request.getCommissionAmount() != null) {
            BigDecimal monthlyRent = request.getMonthlyRentAmount() != null ? request.getMonthlyRentAmount() : contract.getMonthlyRentAmount();
            if (request.getCommissionAmount().compareTo(monthlyRent) >= 0) {
                throw new BadRequestException("Commission amount must be less than monthly rent amount");
            }
            contract.setCommissionAmount(request.getCommissionAmount());
        }
        if (request.getSecurityDepositAmount() != null) {
            contract.setSecurityDepositAmount(request.getSecurityDepositAmount());
        }
        if (request.getLatePaymentPenaltyRate() != null) {
            contract.setLatePaymentPenaltyRate(request.getLatePaymentPenaltyRate());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
            contract.setEndDate(request.getStartDate().plusMonths(contract.getMonthCount()));
        }
        if (request.getSpecialTerms() != null) {
            contract.setSpecialTerms(request.getSpecialTerms());
        }

        RentalContract saved = rentalContractRepository.save(contract);
        log.info("Updated rental contract {}", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRentalContract(UUID contractId) {
        RentalContract contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be deleted");
        }

        checkWriteAccess(contract);

        rentalContractRepository.delete(contract);
        log.info("Deleted rental contract {}", contractId);
    }

    // ==============================
    // RENTAL CONTRACT TRANSITIONS
    // ==============================

    @Override
    @Transactional
    public RentalContractDetailResponse approveRentalContract(UUID contractId) {
        RentalContract contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.DRAFT) {
            throw new BadRequestException("Only DRAFT contracts can be approved");
        }

        checkWriteAccess(contract);

        contract.setStatus(ContractStatusEnum.WAITING_OFFICIAL);

        RentalContract saved = rentalContractRepository.save(contract);
        log.info("Approved rental contract {} -> WAITING_OFFICIAL", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public RentalContractDetailResponse createSecurityDepositPayment(UUID contractId) {
        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.WAITING_OFFICIAL) {
            throw new BadRequestException("Security deposit payment can only be created when contract is in WAITING_OFFICIAL state");
        }

        checkWriteAccess(contract);

        if (contract.getSecurityDepositAmount() == null ||
            contract.getSecurityDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Security deposit amount must be greater than zero to create payment");
        }

        boolean hasSecurityDepositPayment = contract.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.SECURITY_DEPOSIT);
        if (hasSecurityDepositPayment) {
            throw new BadRequestException("Security deposit payment already exists for this contract");
        }

        Payment payment = createContractPayment(
                contract,
                PaymentTypeEnum.SECURITY_DEPOSIT,
                contract.getSecurityDepositAmount(),
                "Security deposit for rental: " + contract.getProperty().getTitle(),
                DEFAULT_PAYMENT_DUE_DAYS
        );

        contract.getPayments().add(payment);

        notificationService.createNotification(
                contract.getCustomer().getUser(),
                NotificationTypeEnum.PAYMENT_DUE,
                "Security Deposit Payment Required",
                String.format("Please complete your security deposit payment of %s VND for property: %s. Due date: %s",
                        contract.getSecurityDepositAmount().toPlainString(),
                        contract.getProperty().getTitle(),
                        payment.getDueDate()),
                RelatedEntityTypeEnum.PAYMENT,
                payment.getId().toString(),
                null
        );

        log.info("Created security deposit payment {} for rental contract {}", payment.getId(), contractId);

        return mapToDetailResponse(contract);
    }

    @Override
    @Transactional
    public RentalContractDetailResponse markRentalPaperworkComplete(UUID contractId) {
        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.WAITING_OFFICIAL) {
            throw new BadRequestException("Paperwork can only be marked complete when contract is in WAITING_OFFICIAL state");
        }

        checkWriteAccess(contract);

        // Check if security deposit is required and paid
        boolean hasSecurityDeposit = contract.getSecurityDepositAmount() != null &&
                contract.getSecurityDepositAmount().compareTo(BigDecimal.ZERO) > 0;

        if (hasSecurityDeposit) {
            boolean securityDepositPaid = contract.getPayments().stream()
                    .anyMatch(p -> p.getPaymentType() == PaymentTypeEnum.SECURITY_DEPOSIT &&
                            (p.getStatus() == PaymentStatusEnum.SUCCESS || p.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS));

            if (!securityDepositPaid) {
                throw new BadRequestException("Security deposit must be paid before marking paperwork complete");
            }
        }

        contract.setSignedAt(LocalDateTime.now());

        // Create first month rent payment
        Payment firstMonthPayment = createContractPayment(
                contract,
                PaymentTypeEnum.MONTHLY,
                contract.getMonthlyRentAmount(),
                "First month rent for: " + contract.getProperty().getTitle(),
                DEFAULT_PAYMENT_DUE_DAYS
        );
        firstMonthPayment.setInstallmentNumber(1);
        paymentRepository.save(firstMonthPayment);

        contract.getPayments().add(firstMonthPayment);
        contract.setStatus(ContractStatusEnum.PENDING_PAYMENT);

        notificationService.createNotification(
                contract.getCustomer().getUser(),
                NotificationTypeEnum.PAYMENT_DUE,
                "First Month Rent Payment Required",
                String.format("Please complete your first month rent payment of %s VND for property: %s. Due date: %s",
                        contract.getMonthlyRentAmount().toPlainString(),
                        contract.getProperty().getTitle(),
                        firstMonthPayment.getDueDate()),
                RelatedEntityTypeEnum.PAYMENT,
                firstMonthPayment.getId().toString(),
                null
        );

        RentalContract saved = rentalContractRepository.save(contract);
        log.info("Marked rental contract {} paperwork complete -> PENDING_PAYMENT, created first month payment", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public RentalContractDetailResponse voidRentalContract(UUID contractId) {
        if (!hasRole(RoleEnum.ADMIN)) {
            throw new ForbiddenException("Only admins can void contracts");
        }

        RentalContract contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() == ContractStatusEnum.CANCELLED ||
            contract.getStatus() == ContractStatusEnum.COMPLETED) {
            throw new BadRequestException("Contract is already in terminal state: " + contract.getStatus());
        }

        contract.setStatus(ContractStatusEnum.CANCELLED);
        contract.setCancellationReason("Voided by admin");
        contract.setCancelledBy(RoleEnum.ADMIN);
        contract.setCancelledAt(LocalDateTime.now());

        RentalContract saved = rentalContractRepository.save(contract);
        log.info("Voided rental contract {} by admin", contractId);

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public RentalContractDetailResponse decideSecurityDeposit(UUID contractId, SecurityDepositDecisionRequest request) {
        if (!hasRole(RoleEnum.ADMIN)) {
            throw new ForbiddenException("Only admins can decide on security deposit");
        }

        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.ACTIVE &&
            contract.getStatus() != ContractStatusEnum.COMPLETED) {
            throw new BadRequestException("Security deposit decision can only be made when contract is ACTIVE or COMPLETED");
        }

        if (contract.getSecurityDepositStatus() != SecurityDepositStatusEnum.HELD) {
            throw new BadRequestException("Security deposit is not in HELD status. Current status: " + contract.getSecurityDepositStatus());
        }

        BigDecimal depositAmount = contract.getSecurityDepositAmount();
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No security deposit amount to transfer");
        }

        contract.setSecurityDepositDecisionAt(LocalDateTime.now());
        contract.setSecurityDepositDecisionReason(request.getReason());

        if (request.getDecision() == SecurityDepositDecisionRequest.SecurityDepositDecision.RETURN_TO_CUSTOMER) {
            triggerPayoutToCustomer(contract, depositAmount, "Security deposit returned to customer");
            contract.setSecurityDepositStatus(SecurityDepositStatusEnum.RETURNED_TO_CUSTOMER);
            log.info("Security deposit for rental contract {} returned to customer", contractId);
        } else {
            triggerPayoutToOwner(contract, depositAmount, "Security deposit transferred to owner");
            contract.setSecurityDepositStatus(SecurityDepositStatusEnum.TRANSFERRED_TO_OWNER);
            log.info("Security deposit for rental contract {} transferred to owner", contractId);
        }

        RentalContract saved = rentalContractRepository.save(contract);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    public void onSecurityDepositPaymentCompleted(UUID contractId) {
        RentalContract contract = rentalContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getSecurityDepositStatus() == SecurityDepositStatusEnum.NOT_PAID) {
            contract.setSecurityDepositStatus(SecurityDepositStatusEnum.HELD);
            rentalContractRepository.save(contract);
            log.info("Security deposit payment completed for rental contract {}, status -> HELD", contractId);
        }
    }

    @Override
    @Transactional
    public void onFirstMonthRentPaymentCompleted(UUID contractId) {
        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.PENDING_PAYMENT) {
            log.warn("First month rent completed but contract {} is not in PENDING_PAYMENT state", contractId);
            return;
        }

        // Transition to ACTIVE
        contract.setStatus(ContractStatusEnum.ACTIVE);

        // Complete linked deposit contract if exists
        if (contract.getDepositContract() != null) {
            try {
                depositContractService.completeDepositContract(contract.getDepositContract().getId());
            } catch (Exception e) {
                log.error("Failed to complete linked deposit contract {}: {}",
                        contract.getDepositContract().getId(), e.getMessage());
            }
        }

        // Payout first month rent to owner (minus commission)
        BigDecimal payoutAmount = contract.getMonthlyRentAmount().subtract(contract.getCommissionAmount());
        var currentTime = LocalDate.now();
        financialUpdateService.transaction(
                contract.getProperty().getId(),
                contract.getCommissionAmount(),
                currentTime.getMonthValue(),
                currentTime.getYear()
        );
        triggerPayoutToOwner(contract, payoutAmount, "First month rent payment");

        rentalContractRepository.save(contract);
        log.info("First month rent completed, rental contract {} -> ACTIVE", contractId);
    }

    @Override
    @Transactional
    public void onMonthlyRentPaymentCompleted(UUID contractId, UUID paymentId) {
        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        // Payout to owner (monthly rent - commission)
        BigDecimal payoutAmount = contract.getMonthlyRentAmount().subtract(contract.getCommissionAmount());
        var currentTime = LocalDate.now();
        financialUpdateService.transaction(
                contract.getProperty().getId(),
                contract.getCommissionAmount(),
                currentTime.getMonthValue(),
                currentTime.getYear()
        );
        triggerPayoutToOwner(contract, payoutAmount, "Monthly rent payment");

        log.info("Monthly rent payment {} completed for rental contract {}, triggered payout to owner", paymentId, contractId);
    }

    @Override
    @Transactional
    public RentalContractDetailResponse completeRentalContract(UUID contractId) {
        RentalContract contract = rentalContractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new NotFoundException("Rental contract not found: " + contractId));

        if (contract.getStatus() != ContractStatusEnum.ACTIVE) {
            throw new BadRequestException("Only ACTIVE contracts can be completed");
        }

        contract.setStatus(ContractStatusEnum.COMPLETED);

        // Send notification about security deposit if still held
        if (contract.getSecurityDepositStatus() == SecurityDepositStatusEnum.HELD) {
            User customerUser = contract.getCustomer().getUser();
            User ownerUser = contract.getProperty().getOwner().getUser();

            String message = String.format(
                    "Rental contract for property '%s' has ended. Please contact admin regarding the security deposit of %s VND.",
                    contract.getProperty().getTitle(),
                    contract.getSecurityDepositAmount().toPlainString()
            );

            notificationService.createNotification(
                    customerUser,
                    NotificationTypeEnum.CONTRACT_UPDATE,
                    "Rental Contract Completed - Security Deposit Pending",
                    message,
                    RelatedEntityTypeEnum.CONTRACT,
                    contractId.toString(),
                    null
            );

            notificationService.createNotification(
                    ownerUser,
                    NotificationTypeEnum.CONTRACT_UPDATE,
                    "Rental Contract Completed - Security Deposit Pending",
                    message,
                    RelatedEntityTypeEnum.CONTRACT,
                    contractId.toString(),
                    null
            );

            log.info("Sent security deposit notifications for completed rental contract {}", contractId);
        }

        RentalContract saved = rentalContractRepository.save(contract);
        log.info("Completed rental contract {}", contractId);

        return mapToDetailResponse(saved);
    }

    // ==================
    // HELPER METHODS
    // ==================

    @Transactional
    public Payment createContractPayment(
            Contract contract, PaymentTypeEnum type, BigDecimal amount, String description, int paymentDueDays
    ) {
        Payment payment = Payment.builder()
                .contract(contract)
                .property(contract.getProperty())
                .payer(contract.getCustomer().getUser())
                .paymentType(type)
                .amount(amount)
                .dueDate(LocalDate.now().plusDays(paymentDueDays))
                .status(Constants.PaymentStatusEnum.PENDING)
                .paymentMethod(PAYOS_METHOD)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

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

    private void checkReadAccess(RentalContract contract) {
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

    private void checkWriteAccess(RentalContract contract) {
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

    private void triggerPayoutToOwner(RentalContract contract, BigDecimal amount, String description) {
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
                    String.format("Cannot process payout of %s VND to property owner '%s' (ID: %s) for rental contract %s due to missing bank details. Please process manually.",
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

        CreatePayoutSessionRequest payoutRequest = CreatePayoutSessionRequest.builder()
                .amount(amount)
                .currency(CURRENCY_VND)
                .accountNumber(ownerUser.getBankAccountNumber())
                .accountHolderName(ownerUser.getBankAccountName())
                .swiftCode(ownerUser.getBankBin())
                .description(description + " - Contract: " + contract.getId())
                .metadata(Map.of(
                        "contractId", contract.getId().toString(),
                        "payoutType", "RENTAL_TO_OWNER"
                ))
                .build();

        paymentGatewayService.createPayoutSession(payoutRequest,
                "payout-owner-" + contract.getId() + "-" + System.currentTimeMillis());

        log.info("Triggered payout to owner {} for rental contract {}: {} VND",
                owner.getId(), contract.getId(), amount);
    }

    private void triggerPayoutToCustomer(RentalContract contract, BigDecimal amount, String description) {
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
                    String.format("Cannot process payout of %s VND to customer '%s' (ID: %s) for rental contract %s due to missing bank details. Please process manually.",
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

        CreatePayoutSessionRequest payoutRequest = CreatePayoutSessionRequest.builder()
                .amount(amount)
                .currency(CURRENCY_VND)
                .accountNumber(customerUser.getBankAccountNumber())
                .accountHolderName(customerUser.getBankAccountName())
                .swiftCode(customerUser.getBankBin())
                .description(description + " - Contract: " + contract.getId())
                .metadata(Map.of(
                        "contractId", contract.getId().toString(),
                        "payoutType", "RENTAL_REFUND_TO_CUSTOMER"
                ))
                .build();

        paymentGatewayService.createPayoutSession(payoutRequest,
                "payout-customer-" + contract.getId() + "-" + System.currentTimeMillis());

        log.info("Triggered payout to customer {} for rental contract {}: {} VND",
                contract.getCustomer().getId(), contract.getId(), amount);
    }

    private Specification<RentalContract> buildRentalContractSpecification(
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

    private RentalContractDetailResponse mapToDetailResponse(RentalContract contract) {
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

        return RentalContractDetailResponse.builder()
                .id(contract.getId())
                .status(contract.getStatus())
                .contractNumber(contract.getContractNumber())
                .monthCount(contract.getMonthCount())
                .monthlyRentAmount(contract.getMonthlyRentAmount())
                .commissionAmount(contract.getCommissionAmount())
                .securityDepositAmount(contract.getSecurityDepositAmount())
                .securityDepositStatus(contract.getSecurityDepositStatus())
                .securityDepositDecisionAt(contract.getSecurityDepositDecisionAt())
                .securityDepositDecisionReason(contract.getSecurityDepositDecisionReason())
                .latePaymentPenaltyRate(contract.getLatePaymentPenaltyRate())
                .accumulatedUnpaidPenalty(contract.getAccumulatedUnpaidPenalty())
                .unpaidMonthsCount(contract.getUnpaidMonthsCount())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
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

    private RentalContractListItem mapToListItem(RentalContract contract) {
        Property property = contract.getProperty();
        Customer customer = contract.getCustomer();
        SaleAgent agent = contract.getAgent();

        String customerName = customer.getUser().getFirstName() + " " + customer.getUser().getLastName();
        String agentName = agent != null
                ? agent.getUser().getFirstName() + " " + agent.getUser().getLastName()
                : null;

        return RentalContractListItem.builder()
                .id(contract.getId())
                .status(contract.getStatus())
                .contractNumber(contract.getContractNumber())
                .monthCount(contract.getMonthCount())
                .monthlyRentAmount(contract.getMonthlyRentAmount())
                .commissionAmount(contract.getCommissionAmount())
                .securityDepositAmount(contract.getSecurityDepositAmount())
                .securityDepositStatus(contract.getSecurityDepositStatus())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
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

