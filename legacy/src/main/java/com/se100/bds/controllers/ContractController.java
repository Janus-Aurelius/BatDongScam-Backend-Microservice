package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.contract.*;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.contract.*;
import com.se100.bds.services.domains.contract.DepositContractService;
import com.se100.bds.services.domains.contract.PurchaseContractService;
import com.se100.bds.services.domains.contract.RentalContractService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contracts")
@Tag(name = "014. Contract Management", description = "Contract CRUD operations, signing, cancellation, and penalties")
@Slf4j
public class ContractController extends AbstractBaseController {

    private final DepositContractService depositContractService;
    private final PurchaseContractService purchaseContractService;
    private final RentalContractService rentalContractService;
    private final UserService userService;

    // =============================
    // DEPOSIT CONTRACT ENDPOINTS
    // =============================

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create a new deposit contract",
            description = "Creates a new deposit contract in DRAFT status. Only admin and sales agents can create. " +
                    "Only one non-DRAFT deposit contract is allowed per property.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contract created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or non-draft deposit contract already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Property, customer, or agent not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> createDepositContract(
            @Valid @RequestBody CreateDepositContractRequest request
    ) {
        var response = depositContractService.createDepositContract(request);
        return responseFactory.successSingle(response, "Deposit contract created successfully");
    }

    @GetMapping("/deposit/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT', 'CUSTOMER', 'PROPERTY_OWNER')")
    @Operation(
            summary = "Get deposit contract by ID",
            description = "Returns detailed deposit contract information. Access is restricted based on role.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contract retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> getDepositContractById(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = depositContractService.getDepositContractById(contractId);
        return responseFactory.successSingle(response, "Deposit contract retrieved successfully");
    }

    @GetMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Get paginated list of deposit contracts",
            description = "Query deposit contracts with various filters. Agents can only see their assigned contracts.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<DepositContractListItem>> getDepositContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses,

            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filter by agent ID")
            @RequestParam(required = false) UUID agentId,

            @Parameter(description = "Filter by property ID")
            @RequestParam(required = false) UUID propertyId,

            @Parameter(description = "Filter by property owner ID")
            @RequestParam(required = false) UUID ownerId,

            @Parameter(description = "Filter by start date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,

            @Parameter(description = "Filter by start date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,

            @Parameter(description = "Filter by end date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateFrom,

            @Parameter(description = "Filter by end date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateTo,

            @Parameter(description = "Search by contract number or property title")
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<DepositContractListItem> contracts = depositContractService.getDepositContracts(
                pageable, statuses, customerId, agentId, propertyId, ownerId,
                startDateFrom, startDateTo, endDateFrom, endDateTo, search
        );

        return responseFactory.successPage(contracts, "Deposit contracts retrieved successfully");
    }

    @PutMapping("/deposit/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Update deposit contract",
            description = "Update deposit contract details. Only allowed for DRAFT status.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contract updated successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> updateDepositContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId,

            @Valid @RequestBody UpdateDepositContractRequest request
    ) {
        var response = depositContractService.updateDepositContract(contractId, request);
        return responseFactory.successSingle(response, "Deposit contract updated successfully");
    }

    @DeleteMapping("/deposit/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Delete deposit contract",
            description = "Hard delete a deposit contract. Only allowed for DRAFT status.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit contract deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<Void>> deleteDepositContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        depositContractService.deleteDepositContract(contractId);
        return responseFactory.successSingle(null, "Deposit contract deleted successfully");
    }

    @PostMapping("/deposit/{contractId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Approve deposit contract",
            description = "Transitions contract from DRAFT to WAITING_OFFICIAL. Agent will handle paperwork after this.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contract approved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> approveDepositContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = depositContractService.approveDepositContract(contractId);
        return responseFactory.successSingle(response, "Deposit contract approved successfully");
    }

    @PostMapping("/deposit/{contractId}/create-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create deposit payment",
            description = "Creates payment for the customer to pay. Only allowed when contract is in WAITING_OFFICIAL state. " +
                    "Calls payment gateway and sends notification to customer.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in WAITING_OFFICIAL status or payment already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> createDepositPayment(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = depositContractService.createDepositPayment(contractId);
        return responseFactory.successSingle(response, "Deposit payment created successfully");
    }

    @PostMapping("/deposit/{contractId}/complete-paperwork")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Mark paperwork complete",
            description = "Marks the legal paperwork as complete. If payment is pending -> PENDING_PAYMENT, " +
                    "if all paid or no payment exists -> auto-creates payment and transitions to PENDING_PAYMENT.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paperwork marked complete",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in WAITING_OFFICIAL status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> markDepositPaperworkComplete(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = depositContractService.markDepositPaperworkComplete(contractId);
        return responseFactory.successSingle(response, "Paperwork marked complete");
    }

    @PostMapping("/deposit/{contractId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER')")
    @Operation(
            summary = "Cancel deposit contract",
            description = "Cancels a deposit contract by customer or owner. " +
                    "Customer cancels: deposit goes to owner. " +
                    "Owner cancels: deposit returns to customer, owner pays penalty.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract cancelled successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract already in terminal state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Only customer or owner can cancel"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> cancelDepositContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId,

            @Valid @RequestBody CancelDepositContractRequest request
    ) {
        var response = depositContractService.cancelDepositContract(contractId, request);
        return responseFactory.successSingle(response, "Deposit contract cancelled successfully");
    }

    @PostMapping("/deposit/{contractId}/void")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Void deposit contract",
            description = "Admin-only operation to void a contract with no side effects (no money transfers).",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract voided successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract already in terminal state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin only"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<DepositContractDetailResponse>> voidDepositContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = depositContractService.voidDepositContract(contractId);
        return responseFactory.successSingle(response, "Deposit contract voided successfully");
    }

    // ==============================
    // PURCHASE CONTRACT ENDPOINTS
    // ==============================

    @PostMapping("/purchase")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create a new purchase contract",
            description = "Creates a new purchase contract in DRAFT status. Only admin and sales agents can create. " +
                    "Only one non-DRAFT purchase contract is allowed per property. " +
                    "If depositContractId is provided, validates deposit is ACTIVE, not expired, and propertyValue matches agreedPrice.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contract created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Property, customer, agent, or deposit contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> createPurchaseContract(
            @Valid @RequestBody CreatePurchaseContractRequest request
    ) {
        var response = purchaseContractService.createPurchaseContract(request);
        return responseFactory.successSingle(response, "Purchase contract created successfully");
    }

    @GetMapping("/purchase/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT', 'CUSTOMER', 'PROPERTY_OWNER')")
    @Operation(
            summary = "Get purchase contract by ID",
            description = "Returns detailed purchase contract information. Access is restricted based on role.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contract retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> getPurchaseContractById(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = purchaseContractService.getPurchaseContractById(contractId);
        return responseFactory.successSingle(response, "Purchase contract retrieved successfully");
    }

    @GetMapping("/purchase")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Get paginated list of purchase contracts",
            description = "Query purchase contracts with various filters. Agents can only see their assigned contracts.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<PurchaseContractListItem>> getPurchaseContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses,

            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filter by agent ID")
            @RequestParam(required = false) UUID agentId,

            @Parameter(description = "Filter by property ID")
            @RequestParam(required = false) UUID propertyId,

            @Parameter(description = "Filter by property owner ID")
            @RequestParam(required = false) UUID ownerId,

            @Parameter(description = "Filter by start date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,

            @Parameter(description = "Filter by start date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,

            @Parameter(description = "Search by contract number or property title")
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<PurchaseContractListItem> contracts = purchaseContractService.getPurchaseContracts(
                pageable, statuses, customerId, agentId, propertyId, ownerId,
                startDateFrom, startDateTo, search
        );

        return responseFactory.successPage(contracts, "Purchase contracts retrieved successfully");
    }

    @PutMapping("/purchase/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Update purchase contract",
            description = "Update purchase contract details. Only allowed for DRAFT status.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contract updated successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> updatePurchaseContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId,

            @Valid @RequestBody UpdatePurchaseContractRequest request
    ) {
        var response = purchaseContractService.updatePurchaseContract(contractId, request);
        return responseFactory.successSingle(response, "Purchase contract updated successfully");
    }

    @DeleteMapping("/purchase/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Delete purchase contract",
            description = "Hard delete a purchase contract. Only allowed for DRAFT status.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Purchase contract deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<Void>> deletePurchaseContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        purchaseContractService.deletePurchaseContract(contractId);
        return responseFactory.successSingle(null, "Purchase contract deleted successfully");
    }

    @PostMapping("/purchase/{contractId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Approve purchase contract",
            description = "Transitions contract from DRAFT to WAITING_OFFICIAL. " +
                    "If advancePaymentAmount > 0, auto-creates advance payment and notifies customer.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contract approved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> approvePurchaseContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = purchaseContractService.approvePurchaseContract(contractId);
        return responseFactory.successSingle(response, "Purchase contract approved successfully");
    }

    @PostMapping("/purchase/{contractId}/complete-paperwork")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Mark paperwork complete",
            description = "Marks the legal paperwork as complete. If remaining amount > 0, creates final payment. " +
                    "Advance payment must be completed first. Transitions to PENDING_PAYMENT or COMPLETED.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paperwork marked complete",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in WAITING_OFFICIAL status or advance payment not completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> markPurchasePaperworkComplete(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = purchaseContractService.markPurchasePaperworkComplete(contractId);
        return responseFactory.successSingle(response, "Paperwork marked complete");
    }

    @PostMapping("/purchase/{contractId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER')")
    @Operation(
            summary = "Cancel purchase contract",
            description = "Cancels a purchase contract by customer or owner. " +
                    "Before payment: nothing happens. After advance payment: refund to customer. " +
                    "After final payment: not allowed (use void instead).",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract cancelled successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract in terminal state or final payment already made"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Only customer or owner can cancel"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> cancelPurchaseContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId,

            @Valid @RequestBody CancelPurchaseContractRequest request
    ) {
        var response = purchaseContractService.cancelPurchaseContract(contractId, request);
        return responseFactory.successSingle(response, "Purchase contract cancelled successfully");
    }

    @PostMapping("/purchase/{contractId}/void")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Void purchase contract",
            description = "Admin-only operation to void a contract with no side effects (no money transfers).",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract voided successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract already in terminal state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin only"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<PurchaseContractDetailResponse>> voidPurchaseContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = purchaseContractService.voidPurchaseContract(contractId);
        return responseFactory.successSingle(response, "Purchase contract voided successfully");
    }

    // ==============================
    // RENTAL CONTRACT ENDPOINTS
    // ==============================

    @PostMapping("/rental")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create a new rental contract",
            description = "Creates a new rental contract in DRAFT status. Only admin and sales agents can create. " +
                    "Only one non-DRAFT rental contract is allowed per property. " +
                    "If depositContractId is provided, validates deposit is ACTIVE, not expired, and monthlyRentAmount matches agreedPrice.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental contract created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Property, customer, agent, or deposit contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> createRentalContract(
            @Valid @RequestBody CreateRentalContractRequest request
    ) {
        var response = rentalContractService.createRentalContract(request);
        return responseFactory.successSingle(response, "Rental contract created successfully");
    }

    @GetMapping("/rental/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT', 'CUSTOMER', 'PROPERTY_OWNER')")
    @Operation(
            summary = "Get rental contract by ID",
            description = "Returns detailed rental contract information. Access is restricted based on role.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental contract retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> getRentalContractById(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = rentalContractService.getRentalContractById(contractId);
        return responseFactory.successSingle(response, "Rental contract retrieved successfully");
    }

    @GetMapping("/rental")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Get paginated list of rental contracts",
            description = "Query rental contracts with various filters. Agents can only see their assigned contracts.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<RentalContractListItem>> getRentalContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses,

            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filter by agent ID")
            @RequestParam(required = false) UUID agentId,

            @Parameter(description = "Filter by property ID")
            @RequestParam(required = false) UUID propertyId,

            @Parameter(description = "Filter by property owner ID")
            @RequestParam(required = false) UUID ownerId,

            @Parameter(description = "Filter by start date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,

            @Parameter(description = "Filter by start date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,

            @Parameter(description = "Filter by end date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateFrom,

            @Parameter(description = "Filter by end date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateTo,

            @Parameter(description = "Search by contract number or property title")
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<RentalContractListItem> contracts = rentalContractService.getRentalContracts(
                pageable, statuses, customerId, agentId, propertyId, ownerId,
                startDateFrom, startDateTo, endDateFrom, endDateTo, search
        );

        return responseFactory.successPage(contracts, "Rental contracts retrieved successfully");
    }

    @PutMapping("/rental/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Update rental contract",
            description = "Update rental contract details. Only allowed for DRAFT status.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental contract updated successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> updateRentalContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId,

            @Valid @RequestBody UpdateRentalContractRequest request
    ) {
        var response = rentalContractService.updateRentalContract(contractId, request);
        return responseFactory.successSingle(response, "Rental contract updated successfully");
    }

    @DeleteMapping("/rental/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Delete rental contract",
            description = "Hard delete a rental contract. Only allowed for DRAFT status.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental contract deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<Void>> deleteRentalContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        rentalContractService.deleteRentalContract(contractId);
        return responseFactory.successSingle(null, "Rental contract deleted successfully");
    }

    @PostMapping("/rental/{contractId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Approve rental contract",
            description = "Transitions contract from DRAFT to WAITING_OFFICIAL.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental contract approved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in DRAFT status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> approveRentalContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = rentalContractService.approveRentalContract(contractId);
        return responseFactory.successSingle(response, "Rental contract approved successfully");
    }

    @PostMapping("/rental/{contractId}/create-security-deposit-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Create security deposit payment",
            description = "Creates security deposit payment for the customer. Only when contract is in WAITING_OFFICIAL state and security deposit amount > 0.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Security deposit payment created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in WAITING_OFFICIAL status or no security deposit"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> createSecurityDepositPayment(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = rentalContractService.createSecurityDepositPayment(contractId);
        return responseFactory.successSingle(response, "Security deposit payment created successfully");
    }

    @PostMapping("/rental/{contractId}/complete-paperwork")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(
            summary = "Mark paperwork complete",
            description = "Marks the legal paperwork as complete. Security deposit must be paid first if amount > 0. " +
                    "Transitions to ACTIVE and completes linked deposit contract.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paperwork marked complete",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in WAITING_OFFICIAL status or security deposit not paid"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> markRentalPaperworkComplete(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = rentalContractService.markRentalPaperworkComplete(contractId);
        return responseFactory.successSingle(response, "Paperwork marked complete");
    }

    @PostMapping("/rental/{contractId}/void")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Void rental contract",
            description = "Admin-only operation to void a contract with no side effects (no money transfers). " +
                    "Customers and owners must contact admin to cancel/void the contract.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contract voided successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract already in terminal state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin only"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> voidRentalContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = rentalContractService.voidRentalContract(contractId);
        return responseFactory.successSingle(response, "Rental contract voided successfully");
    }

    @PostMapping("/rental/{contractId}/decide-security-deposit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Decide security deposit destination",
            description = "Admin-only operation to decide where the security deposit goes (return to customer or transfer to owner). " +
                    "Only callable when contract is ACTIVE or COMPLETED and security deposit is HELD.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Security deposit decision made successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in valid state or security deposit not held"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin only"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> decideSecurityDeposit(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId,

            @Valid @RequestBody SecurityDepositDecisionRequest request
    ) {
        var response = rentalContractService.decideSecurityDeposit(contractId, request);
        return responseFactory.successSingle(response, "Security deposit decision made successfully");
    }

    @PostMapping("/rental/{contractId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Complete rental contract",
            description = "Admin-only operation to complete a rental contract when rental period ends. " +
                    "Transitions from ACTIVE to COMPLETED and sends notifications about security deposit.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rental contract completed successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Contract not in ACTIVE state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin only"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    public ResponseEntity<SingleResponse<RentalContractDetailResponse>> completeRentalContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable UUID contractId
    ) {
        var response = rentalContractService.completeRentalContract(contractId);
        return responseFactory.successSingle(response, "Rental contract completed successfully");
    }

    // =============================
    // CUSTOMER - MY CONTRACTS
    // =============================

    @GetMapping("/my/deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get customer's deposit contracts",
            description = "Returns paginated list of deposit contracts where the current user is the customer.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<DepositContractListItem>> getMyDepositContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        Page<DepositContractListItem> contracts = depositContractService.getDepositContracts(
                pageable, statuses, currentUserId, null, null, null,
                null, null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your deposit contracts retrieved successfully");
    }

    @GetMapping("/my/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get customer's purchase contracts",
            description = "Returns paginated list of purchase contracts where the current user is the customer.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<PurchaseContractListItem>> getMyPurchaseContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        Page<PurchaseContractListItem> contracts = purchaseContractService.getPurchaseContracts(
                pageable, statuses, currentUserId, null, null, null,
                null, null, null
        );

        return responseFactory.successPage(contracts, "Your purchase contracts retrieved successfully");
    }

    @GetMapping("/my/rental")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get customer's rental contracts",
            description = "Returns paginated list of rental contracts where the current user is the customer.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<RentalContractListItem>> getMyRentalContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        Page<RentalContractListItem> contracts = rentalContractService.getRentalContracts(
                pageable, statuses, currentUserId, null, null, null,
                null, null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your deposit contracts retrieved successfully");
    }

    // =============================
    // PROPERTY OWNER - MY PROPERTIES CONTRACTS
    // =============================

    @GetMapping("/my-properties/deposit")
    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    @Operation(
            summary = "Get owner's property deposit contracts",
            description = "Returns paginated list of deposit contracts for properties owned by the current user.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<DepositContractListItem>> getMyPropertiesDepositContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        // Filter by ownerId - the owner of the property
        Page<DepositContractListItem> contracts = depositContractService.getDepositContracts(
                pageable, statuses, null, null, null, currentUserId,
                null, null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your property deposit contracts retrieved successfully");
    }

    @GetMapping("/my-properties/purchase")
    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    @Operation(
            summary = "Get owner's property purchase contracts",
            description = "Returns paginated list of purchase contracts for properties owned by the current user.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<PurchaseContractListItem>> getMyPropertiesPurchaseContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        // Filter by ownerId - the owner of the property
        Page<PurchaseContractListItem> contracts = purchaseContractService.getPurchaseContracts(
                pageable, statuses, null, null, null, currentUserId,
                null, null, null
        );

        return responseFactory.successPage(contracts, "Your property purchase contracts retrieved successfully");
    }

    @GetMapping("/my-properties/rental")
    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    @Operation(
            summary = "Get owner's property rental contracts",
            description = "Returns paginated list of rental contracts for properties owned by the current user.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<RentalContractListItem>> getMyPropertiesRentalContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        // Filter by ownerId - the owner of the property
        Page<RentalContractListItem> contracts = rentalContractService.getRentalContracts(
                pageable, statuses, null, null, null, currentUserId,
                null, null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your property purchase contracts retrieved successfully");
    }

    // =============================
    // SALES AGENT - MY ASSIGNED CONTRACTS
    // =============================

    @GetMapping("/my-assigned/deposit")
    @PreAuthorize("hasRole('SALESAGENT')")
    @Operation(
            summary = "Get agent's assigned deposit contracts",
            description = "Returns paginated list of deposit contracts where the current user is the assigned agent.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<DepositContractListItem>> getMyAssignedDepositContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        Page<DepositContractListItem> contracts = depositContractService.getDepositContracts(
                pageable, statuses, null, currentUserId, null, null,
                null, null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your assigned deposit contracts retrieved successfully");
    }

    @GetMapping("/my-assigned/purchase")
    @PreAuthorize("hasRole('SALESAGENT')")
    @Operation(
            summary = "Get agent's assigned purchase contracts",
            description = "Returns paginated list of purchase contracts where the current user is the assigned agent.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Purchase contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<PurchaseContractListItem>> getMyAssignedPurchaseContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        Page<PurchaseContractListItem> contracts = purchaseContractService.getPurchaseContracts(
                pageable, statuses, null, currentUserId, null,
                null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your assigned purchase contracts retrieved successfully");
    }

    @GetMapping("/my-assigned/rental")
    @PreAuthorize("hasRole('SALESAGENT')")
    @Operation(
            summary = "Get agent's assigned rental contracts",
            description = "Returns paginated list of rental contracts where the current user is the assigned agent.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit contracts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<RentalContractListItem>> getMyAssignedRentalContracts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,

            @Parameter(description = "Filter by statuses")
            @RequestParam(required = false) List<ContractStatusEnum> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        UUID currentUserId = userService.getUserId();

        Page<RentalContractListItem> contracts = rentalContractService.getRentalContracts(
                pageable, statuses, null, currentUserId, null, null,
                null, null, null, null, null
        );

        return responseFactory.successPage(contracts, "Your assigned deposit contracts retrieved successfully");
    }
}
