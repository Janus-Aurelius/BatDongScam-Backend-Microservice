package com.se.bds.core.property.internal.adapter.in.web;

import com.se.bds.core.property.internal.adapter.in.web.dto.CreatePropertyWebRequest;
import com.se.bds.core.property.internal.adapter.in.web.dto.PropertyTypeWebRequest;
import com.se.bds.core.property.internal.adapter.in.web.dto.UpdatePropertyStatusWebRequest;
import com.se.bds.core.property.internal.adapter.in.web.dto.UpdatePropertyWebRequest;
import com.se.bds.core.property.internal.adapter.in.web.mapper.PropertyWebMapper;
import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.property.internal.domain.model.PropertyType;
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
public class PropertyController {
    private final PropertyUseCase propertyUseCase;
    private final PropertyWebMapper propertyWebMapper;

    // core property management - admin/owner
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    @PostMapping (value = "/properties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    @PutMapping (value = "/properties/{properyId}/status")
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
            @RequestPart(required = false)List<UUID> cityIds,
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
            Pageable pageable
            )
    {
        SearchPropertyCommand command = new SearchPropertyCommand(
                cityIds, districtIds, wardIds, propertyTypeIds, ownerId, agentId,
                minPrice, maxPrice, minArea, maxArea, rooms, bathrooms, bedrooms, floors,
                transactionType, statuses
        );
        return ResponseEntity.ok(propertyUseCase.searchProperties(command, pageable));
    }

    @GetMapping("/public/properties/{propertyId")
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

    @GetMapping("/public/properties/types")
    public ResponseEntity<Page<PropertyType>> getAllPropertyTypes(Pageable pageable)
    {
        return ResponseEntity.ok(propertyUseCase.getAllPropertyTypes(pageable));
    }

}
