package com.se.bds.core.property.internal.v2.web;

import com.se.bds.core.property.internal.domain.model.PropertyStatus;
import com.se.bds.core.property.internal.domain.model.TransactionType;
import com.se.bds.core.property.internal.v2.domain.PropertyReadModel;
import com.se.bds.core.property.internal.v2.repository.PropertyReadModelRepository;
import com.se.bds.core.property.internal.v2.service.PropertyCommandHandler;
import com.se.bds.core.property.internal.v2.service.PropertyCommandHandler.CreatePropertyCommand;
import com.se.bds.core.property.internal.v2.service.PropertyCommandHandler.UpdatePropertyStatusCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller to expose v2 API endpoints leveraging CQRS & Event Sourcing patterns.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/properties")
@Tag(name = "Core - Properties v2 (CQRS & Event Sourcing)", description = "V2 property management endpoints leveraging CQRS and Event Sourcing")
public class PropertyControllerV2 {

    private final PropertyCommandHandler commandHandler;
    private final PropertyReadModelRepository readModelRepository;

    public record CreatePropertyWebRequestV2(
            @NotNull UUID ownerId,
            @NotNull UUID wardId,
            @NotBlank String title,
            String description,
            @NotNull @Positive BigDecimal priceAmount,
            @NotBlank String transactionType
    ) {}

    public record UpdatePropertyStatusWebRequestV2(
            @NotBlank String status
    ) {}

    /**
     * Create a new property listing using the write model command handler.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    @PostMapping
    @Operation(summary = "Create a new property listing (CQRS)", description = "Creates a property listing by writing to the Event Store and updating read projections.")
    public ResponseEntity<UUID> createProperty(@Valid @RequestBody CreatePropertyWebRequestV2 request) {
        UUID propertyId = UUID.randomUUID();
        CreatePropertyCommand command = new CreatePropertyCommand(
                propertyId,
                request.ownerId(),
                request.wardId(),
                request.title(),
                request.description(),
                request.priceAmount(),
                TransactionType.valueOf(request.transactionType().toUpperCase())
        );
        UUID createdId = commandHandler.handleCreate(command);
        return ResponseEntity.ok(createdId);
    }

    /**
     * Update the lifecycle status of a property using the write model command handler.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    @PutMapping("/{propertyId}/status")
    @Operation(summary = "Update property status (CQRS)", description = "Modifies property status by appending a transition event to the Event Store history.")
    public ResponseEntity<Void> updatePropertyStatus(
            @PathVariable UUID propertyId,
            @Valid @RequestBody UpdatePropertyStatusWebRequestV2 request
    ) {
        UpdatePropertyStatusCommand command = new UpdatePropertyStatusCommand(
                propertyId,
                PropertyStatus.valueOf(request.status().toUpperCase())
        );
        commandHandler.handleUpdateStatus(command);
        return ResponseEntity.ok().build();
    }

    /**
     * Get property details directly from the flat query projection view.
     */
    @GetMapping("/{propertyId}")
    @Operation(summary = "Get property details (CQRS Read Path)", description = "Gets property details directly from the flat query projection view.")
    public ResponseEntity<PropertyReadModel> getPropertyDetails(@PathVariable UUID propertyId) {
        return readModelRepository.findById(propertyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
