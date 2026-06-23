package com.se.bds.core.property.internal.adapter.in.web;

import com.se.bds.core.property.internal.adapter.in.web.dto.*;
import com.se.bds.core.property.internal.adapter.in.web.mapper.PropertyWebMapper;
import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Core - Properties", description = "Endpoints for property management, search, and metadata")
public class PropertyController {
    private final PropertyUseCase propertyUseCase;
    private final PropertyWebMapper propertyWebMapper;

    // core property management - admin/owner
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    @PostMapping (value = "/properties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new property listing", description = "Creates a property listing with images and documents. Requires multipart/form-data.")
    public ResponseEntity<Property> createProperty (
            @Valid @RequestPart("payload")CreatePropertyWebRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestPart(value = "documents", required = false) MultipartFile[] documents
            )
    {
        CreatePropertyCommand command = propertyWebMapper.toCreatePropertyCommand(request);
        return ResponseEntity.ok(propertyUseCase.createProperty(command, images, documents));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    @PutMapping (value = "/properties/{propertyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update property listing", description = "Updates an existing property listing's details and files.")
    public ResponseEntity<Property> updateProperty (
            @Valid @RequestPart("payload")UpdatePropertyWebRequest request,
            @RequestPart(value = "images",required = false) MultipartFile[] images,
            @RequestPart(value = "documents", required = false) MultipartFile[] documents,
            @PathVariable UUID propertyId)
    {
        UpdatePropertyCommand command = propertyWebMapper.toUpdatePropertyCommand(request);
        return ResponseEntity.ok(propertyUseCase.updateProperty(propertyId, command,images,documents));
    }

    // TODO check the business context of this action
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    @PutMapping (value = "/properties/{propertyId}/status")
    @Operation(summary = "Update property status", description = "Changes the lifecycle status of a property (e.g., ACTIVE, SOLD, RENTED).")
    public ResponseEntity<Property> updatePropertyStatus(
            @PathVariable UUID propertyId,
            @Valid @RequestBody UpdatePropertyStatusWebRequest request
            )
    {
        UpdatePropertyStatusCommand command = new UpdatePropertyStatusCommand((request.status()));
        return ResponseEntity.ok(propertyUseCase.updatePropertyStatus(propertyId, command));
    }

    @PreAuthorize(("hasRole('ADMIN')"))
    @DeleteMapping ("/properties/{propertyId}")
    public ResponseEntity<Void> deleteProperty (@PathVariable UUID propertyId)
    {
        propertyUseCase.deleteProperty(propertyId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping ("/properties/{properties}/assign-agent/{agentId}")
    public ResponseEntity<Void> assignAgent(@PathVariable UUID properties, @PathVariable UUID agentId)
    {
        propertyUseCase.assignAgent(properties, agentId);
        return ResponseEntity.ok().build();
    }

    //public queries/ searches

    @GetMapping("/public/properties/search")
    public ResponseEntity<Page<?>> searchProperties(
            @RequestParam(required = false)List<UUID> cityIds,
            @RequestParam(required = false) List<UUID> districtIds,
            @RequestParam(required = false) List<UUID> wardIds,
            @RequestParam(required = false) List<UUID> propertyTypeIds,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) Integer rooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer floors,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Boolean topK,
            Pageable pageable
            )
    {
        SearchPropertyCommand command = new SearchPropertyCommand(
                cityIds, districtIds, wardIds, propertyTypeIds, ownerId, agentId,
                minPrice, maxPrice, minArea, maxArea, rooms, bathrooms, bedrooms, floors,
                transactionType, statuses, topK
        );
        return ResponseEntity.ok(propertyUseCase.searchProperties(command, pageable));
    }

    @GetMapping("/public/properties/{propertyId}")
    public ResponseEntity<?> getPropertyDetails(@PathVariable UUID propertyId)
    {
        return ResponseEntity.ok(propertyUseCase.getPropertyDetail(propertyId));
    }

    //property types crud by admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/properties/types")
    public ResponseEntity<?> createPropertyType(@Valid @RequestBody PropertyTypeWebRequest request)
    {
        return ResponseEntity.ok(propertyUseCase.createPropertyType(new CreatePropertyTypeCommand(
                request.typeName(), request.description(), request.isActive()
        )));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/properties/types/{typeId}")
    public ResponseEntity<?> updatePropertyType(@PathVariable UUID typeId, @Valid @RequestBody PropertyTypeWebRequest request) {
        return ResponseEntity.ok(propertyUseCase.updatePropertyType(typeId, new UpdatePropertyTypeCommand(
                request.typeName(), request.description(), request.isActive()
        )));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/properties/types/{typeId}")
    public ResponseEntity<Void> deletePropertyType(@PathVariable UUID typeId) {
        propertyUseCase.deletePropertyType(typeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/properties/types")
    public ResponseEntity<Page<PropertyType>> getAllPropertyTypes(Pageable pageable)
    {
        return ResponseEntity.ok(propertyUseCase.getAllPropertyTypes(pageable));
    }

    // Document Type CRUD
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/properties/documents/types")
    public ResponseEntity<?> createDocumentType(@Valid @RequestBody CreateDocumentTypeWebRequest request) {
        return ResponseEntity.ok(propertyUseCase.createDocumentType(new CreateDocumentTypeCommand(
                request.name(), request.description(), request.isCompulsory()
        )));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/properties/documents/types/{typeId}")
    public ResponseEntity<?> updateDocumentType(@PathVariable UUID typeId, @Valid @RequestBody UpdateDocumentTypeWebRequest request) {
        return ResponseEntity.ok(propertyUseCase.updateDocumentType(typeId, new UpdateDocumentTypeCommand(
                request.name(), request.description(), request.isCompulsory()
        )));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/properties/documents/types/{typeId}")
    public ResponseEntity<Void> deleteDocumentType(@PathVariable UUID typeId) {
        propertyUseCase.deleteDocumentType(typeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/properties/documents/types")
    public ResponseEntity<?> getAllDocumentTypes(Pageable pageable) {
        return ResponseEntity.ok(propertyUseCase.getAllDocumentTypes(pageable));
    }

    @GetMapping("/public/properties/documents/types/list")
    public ResponseEntity<?> getAllDocumentTypesList() {
        return ResponseEntity.ok(propertyUseCase.getAllDocumentTypesList());
    }

    // Property Documents Upload & Verification
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    @PostMapping(value = "/properties/{propertyId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPropertyDocument(
            @PathVariable UUID propertyId,
            @RequestPart("payload") DocumentMetadataWebRequest request,
            @RequestPart("file") MultipartFile file) {
        UploadDocumentCommand command = new UploadDocumentCommand(
                request.documentTypeId(), request.documentNumber(), request.documentName(),
                request.issueDate(), request.expiryDate(), request.issuingAuthority(), request.fileIndex()
        );
        return ResponseEntity.ok(propertyUseCase.uploadPropertyDocument(propertyId, command, file));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    @GetMapping("/properties/{propertyId}/documents")
    public ResponseEntity<?> getPropertyDocuments(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(propertyUseCase.getPropertyDocuments(propertyId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/properties/documents/{documentId}/verify")
    public ResponseEntity<?> verifyPropertyDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody VerifyDocumentWebRequest request) {
        VerifyDocumentCommand command = new VerifyDocumentCommand(request.status(), request.rejectionReason());
        return ResponseEntity.ok(propertyUseCase.verifyPropertyDocument(documentId, command));
    }

    @GetMapping("/api/internal/property-types/ids")
    public ResponseEntity<List<UUID>> getAllAvailablePropertyTypeIds() {
        return ResponseEntity.ok(propertyUseCase.getAllAvailablePropertyTypeIds());
    }

    @GetMapping("/api/internal/properties/{propertyId}/location-info")
    public ResponseEntity<PropertyUseCase.PropertyLocationInfo> getPropertyLocationInfo(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(propertyUseCase.getPropertyLocationInfo(propertyId));
    }
}
