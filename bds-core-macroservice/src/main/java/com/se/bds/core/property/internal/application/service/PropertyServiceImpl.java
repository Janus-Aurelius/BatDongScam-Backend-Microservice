package com.se.bds.core.property.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.discovery.MSG32;
import com.se.bds.common.message.validation.MSG11;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.common.message.validation.MSG2;
import com.se.bds.core.property.api.event.*;
import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.application.command.pattern.CreatePropertyAction;
import com.se.bds.core.property.internal.application.command.pattern.DeletePropertyAction;
import com.se.bds.core.property.internal.application.command.pattern.UpdatePropertyAction;
import com.se.bds.core.property.internal.application.port.in.PropertyUseCase;
import com.se.bds.core.property.internal.application.port.out.MessagePublisherPort;
import com.se.bds.core.property.internal.domain.model.*;
import com.se.bds.core.property.internal.domain.model.strategy.FeeCalculationStrategy;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.application.port.out.PropertyTypeRepository;
import com.se.bds.core.property.internal.application.port.out.DocumentTypeRepository;
import com.se.bds.core.property.internal.application.port.out.IdentificationDocumentRepository;
import com.se.bds.core.property.internal.application.port.out.PropertyFileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.lang.String.valueOf;

@Service
@RequiredArgsConstructor
@Slf4j
class PropertyServiceImpl implements PropertyUseCase {
    private final PropertyRepository propertyRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final MessagePublisherPort messagePublisherPort;

    private final List<FeeCalculationStrategy> feeStrategies;

    private final DocumentTypeRepository documentTypeRepository;
    private final IdentificationDocumentRepository identificationDocumentRepository;
    private final PropertyFileStoragePort fileStoragePort;

    @Override
    @Transactional
    public Property createProperty(CreatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents)
    {
        // BR19: Validate file sizes
        if (mediaFiles != null) {
            for (MultipartFile file : mediaFiles) {
                if (file.getSize() > 5 * 1024 * 1024) { // 5MB
                    throw new BusinessException(MSG11.CODE, MSG11.MESSAGE);
                }
            }
        }

        validateCompulsoryDocuments(command.documentsMetadata(), isUserAdmin());

        Property property = new Property();
        property.setOwnerId(command.ownerId());
        PropertyType propertyType = propertyTypeRepository.findById(command.propertyTypeId())
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
        property.setPropertyType(propertyType);
        property.setWardId(command.wardId());
        property.setTitle(command.title());
        property.setDescription(command.description());
        property.setPriceAmount(command.priceAmount());
        property.setArea(command.area());
        property.setRooms(command.rooms());
        property.setBedrooms(command.bedrooms());
        property.setBathrooms(command.bathrooms());
        property.setFloors(command.floors());

        if (command.houseOrientation() != null) {
            property.setHouseOrientation(Orientation.valueOf(command.houseOrientation()));
        }
        if (command.balconyOrientation() != null) {
            property.setBalconyOrientation(Orientation.valueOf(command.balconyOrientation()));
        }
        if (command.transactionType() != null) {
            property.setTransactionType(TransactionType.valueOf(command.transactionType()));
        }

        property.setFullAddress(command.address());
        property.setStatus(PropertyStatus.PENDING);

        if (command.priceAmount() != null && command.area() != null && command.area().compareTo(java.math.BigDecimal.ZERO) > 0) {
            property.setPricePerSquareMeter(command.priceAmount().divide(command.area(), 2, java.math.RoundingMode.HALF_UP));
        }

        // Apply STRATEGY PATTERN
        FeeCalculationStrategy feeStrategy = feeStrategies.stream()
                .filter(s -> s.supports(property.getTransactionType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(MSG12.CODE, "No valid fee strategy found for this transaction"));

        property.setCommissionRate(feeStrategy.calculateCommissionRate());
        property.setServiceFeeAmount(feeStrategy.calculateServiceFee(command.priceAmount(), command.area()));
        property.setServiceFeeCollectedAmount(java.math.BigDecimal.ZERO);

        // Apply COMMAND PATTERN
        CreatePropertyAction createAction = new CreatePropertyAction(propertyRepository, property);
        Property saved = createAction.execute();

        // Handle file uploads
        addMediaFiles(saved, mediaFiles);
        addDocumentFiles(saved, documents, command.documentsMetadata());

        Property savedWithFiles = propertyRepository.save(saved);

        // Apply OBSERVER PATTERN
        PropertyCreatedIntegrationEvent integrationEvent = new PropertyCreatedIntegrationEvent(
                savedWithFiles.getId(),
                savedWithFiles.getTitle(),
                savedWithFiles.getOwnerId(),
                savedWithFiles.getTransactionType().name()
        );
        messagePublisherPort.publishPropertyCreated(integrationEvent);

        return savedWithFiles;
    }

    /**
     * @param command
     * @param mediaFiles
     * @param documents
     * @return
     */
    @Override
    @Transactional
    public Property updateProperty(
            UUID propertyId,
            UpdatePropertyCommand command,
            MultipartFile[] mediaFiles,
            MultipartFile[] documents
    ) {
        Property property = getProperty(propertyId);

        if (property.getStatus() == PropertyStatus.DELETED) {
            throw new IllegalStateException("Cannot update a deleted property");
        }

        UpdatePropertyAction action = new UpdatePropertyAction(propertyRepository, property);

        property.setTitle(command.title());
        property.setDescription(command.description());
        property.setPriceAmount(command.priceAmount());
        property.setArea(command.area());

        PropertyType propertyType = propertyTypeRepository.findById(command.propertyTypeId())
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
        property.setPropertyType(propertyType);

        if (command.wardId() != null) {
            property.setWardId(command.wardId());
        }
        if (command.rooms() != null) {
            property.setRooms(command.rooms());
        }
        if (command.bedrooms() != null) {
            property.setBedrooms(command.bedrooms());
        }
        if (command.bathrooms() != null) {
            property.setBathrooms(command.bathrooms());
        }
        if (command.floors() != null) {
            property.setFloors(command.floors());
        }
        if (command.houseOrientation() != null) {
            property.setHouseOrientation(Orientation.valueOf(command.houseOrientation()));
        }
        if (command.balconyOrientation() != null) {
            property.setBalconyOrientation(Orientation.valueOf(command.balconyOrientation()));
        }
        if (command.transactionType() != null) {
            property.setTransactionType(TransactionType.valueOf(command.transactionType()));
        }
        property.setFullAddress(command.address());

        if (command.priceAmount() != null && command.area() != null && command.area().compareTo(java.math.BigDecimal.ZERO) > 0) {
            property.setPricePerSquareMeter(command.priceAmount().divide(command.area(), 2, java.math.RoundingMode.HALF_UP));
        }

        // Apply STRATEGY PATTERN
        FeeCalculationStrategy feeStrategy = feeStrategies.stream()
                .filter(s -> s.supports(property.getTransactionType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(MSG12.CODE, "No valid fee strategy found for this transaction"));

        property.setCommissionRate(feeStrategy.calculateCommissionRate());
        property.setServiceFeeAmount(feeStrategy.calculateServiceFee(command.priceAmount(), command.area()));

        boolean isAdmin = isUserAdmin();
        if (isAdmin) {
            property.setServiceFeeCollectedAmount(property.getServiceFeeAmount());
            property.setStatus(PropertyStatus.AVAILABLE);
            if (property.getApprovedAt() == null) {
                property.setApprovedAt(LocalDateTime.now());
            }
        } else {
            // force re-approval
            property.setStatus(PropertyStatus.PENDING);
        }

        ensureMediaCollection(property);
        ensureDocumentCollection(property);

        removeMediaFiles(property, command.mediaIdsToRemove());
        removeDocumentFiles(property, command.documentIdsToRemove());

        addMediaFiles(property, mediaFiles);
        addDocumentFiles(property, documents, command.documentsMetadata());

        validatePropertyHasCompulsoryDocuments(property, isAdmin);

        Property saved = action.execute();

        messagePublisherPort.publishPropertyUpdated(new PropertyUpdatedIntegrationEvent(saved.getId()));

        return saved;
    }

    /**
     * @param propertyId
     * @param command
     * @return
     */
    @Override
    public Property updatePropertyStatus(UUID propertyId, UpdatePropertyStatusCommand command) {
        Property property = getProperty(propertyId);
        PropertyStatus newStatus = PropertyStatus.valueOf(command.targetStatus());
        PropertyStatus oldStatus = property.transitionStatus(newStatus);

        Property saved = propertyRepository.save(property);

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
                new PropertyId(propertyId), oldStatus.name(), newStatus.name(), Instant.now()
        ));
        return saved;
    }

    /**
     * @param propertyId
     */
    @Override
    @Transactional
    public void deleteProperty(UUID propertyId) {
        Property property = getProperty(propertyId);

        PropertyStatus oldStatus = property.getStatus();

        DeletePropertyAction action = new DeletePropertyAction(propertyRepository, property);
        action.execute();

        eventPublisher.publishEvent(new PropertyStatusChangedEvent(
           new PropertyId(propertyId), oldStatus.name(), PropertyStatus.DELETED.name(), Instant.now()
        ));

        messagePublisherPort.publishPropertyDeleted(new PropertyDeletedIntegrationEvent(propertyId));
    }

    /**
     * @param propertyId
     * @param agentId
     */
    @Override
    @Transactional
    public void assignAgent(UUID propertyId, UUID agentId) {
        Property property = getProperty(propertyId);
        UUID previousAgentId = property.assignAgent(agentId);
        propertyRepository.save(property);

        eventPublisher.publishEvent(new PropertyAgentAssignedEvent(
                new PropertyId(propertyId), agentId, previousAgentId, Instant.now()
        ));
    }

    /**
     * @param command
     * @param pageable
     * @return
     */
    @Override
    public Page<Property> searchProperties(SearchPropertyCommand command, Pageable pageable) {
        return propertyRepository.searchWithFilters(
                command.cityIds(),command.districtIds(),command.wardIds(),command.propertyTypeIds(),
                command.ownerId(),command.agentId(),command.minPrice(),command.maxPrice(),command.minArea(),
                command.maxArea(),pageable
        );
    }

    /**
     * @param propertyId
     * @return
     */
    @Override
    @Cacheable(value = "propertyDetails", key = "#propertyId")
    public Property getPropertyDetail(UUID propertyId) {
        return getProperty(propertyId);
    }

    /**
     * @param command
     * @return
     */
    @Override
    public PropertyType createPropertyType(CreatePropertyTypeCommand command) {
        PropertyType propertyType = new PropertyType();
        propertyType.setTypeName(command.typeName());
        propertyType.setDescription(command.description());
        propertyType.setIsActive(command.isActive());
        return propertyTypeRepository.save(propertyType);
    }

    /**
     * @param pageable
     * @return
     */
    @Override
    public Page<PropertyType> getAllPropertyTypes(Pageable pageable) {
        return propertyTypeRepository.findAll(pageable);
    }

    @Override
    public PropertyType updatePropertyType(UUID id, UpdatePropertyTypeCommand command) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));

        if (command.typeName() != null) propertyType.setTypeName(command.typeName());
        if (command.description() != null) propertyType.setDescription(command.description());
        if (command.isActive() != null) propertyType.setIsActive(command.isActive());

        return propertyTypeRepository.save(propertyType);
    }

    @Override
    public void deletePropertyType(UUID id) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
        propertyTypeRepository.delete(propertyType);
    }

    @Override
    public java.util.List<UUID> getAllAvailablePropertyTypeIds() {
        return propertyTypeRepository.getAllActiveIds();
    }

    @Override
    public String getPropertyTypeName(UUID propertyTypeId) {
        return propertyTypeRepository.findById(propertyTypeId)
            .map(PropertyType::getTypeName)
            .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
    }

    @Override
    public int countPropertiesByPropertyTypeId(UUID propertyTypeId) {
        return propertyRepository.countByPropertyTypeId(propertyTypeId);
    }

    // Document Type CRUD
    @Override
    @Transactional
    public DocumentType createDocumentType(CreateDocumentTypeCommand command) {
        DocumentType documentType = DocumentType.builder()
                .name(command.name())
                .description(command.description())
                .isCompulsory(command.isCompulsory())
                .build();
        return documentTypeRepository.save(documentType);
    }

    @Override
    @Transactional
    public DocumentType updateDocumentType(UUID id, UpdateDocumentTypeCommand command) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, "Document type not found with id: " + id));

        if (command.name() != null) {
            documentType.setName(command.name());
        }
        if (command.description() != null) {
            documentType.setDescription(command.description());
        }
        if (command.isCompulsory() != null) {
            documentType.setIsCompulsory(command.isCompulsory());
        }

        return documentTypeRepository.save(documentType);
    }

    @Override
    @Transactional
    public void deleteDocumentType(UUID id) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, "Document type not found with id: " + id));
        documentTypeRepository.delete(documentType);
    }

    @Override
    public Page<DocumentType> getAllDocumentTypes(Pageable pageable) {
        return documentTypeRepository.findAll(pageable);
    }

    @Override
    public java.util.List<DocumentType> getAllDocumentTypesList() {
        return documentTypeRepository.findAllList();
    }

    // Property documents
    @Override
    @Transactional
    public IdentificationDocument uploadPropertyDocument(UUID propertyId, UploadDocumentCommand command, MultipartFile file) {
        Property property = getProperty(propertyId);
        ensureDocumentCollection(property);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            byte[] bytes = file.getBytes();
            String fileUrl = fileStoragePort.uploadFile(bytes, "properties/" + property.getId() + "/documents", file.getOriginalFilename());
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            DocumentType documentType = documentTypeRepository.findById(command.documentTypeId())
                    .orElseThrow(() -> new BusinessException(MSG18.CODE, "Document type not found with id: " + command.documentTypeId()));

            String documentNumber = (command.documentNumber() != null && !command.documentNumber().isBlank())
                    ? command.documentNumber().substring(0, Math.min(command.documentNumber().length(), 20))
                    : "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            String documentName = (command.documentName() != null && !command.documentName().isBlank())
                    ? command.documentName()
                    : (file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getName());

            IdentificationDocument document = IdentificationDocument.builder()
                    .documentType(documentType)
                    .property(property)
                    .documentNumber(documentNumber)
                    .documentName(documentName)
                    .filePath(fileUrl)
                    .mimeType(mimeType)
                    .issueDate(command.issueDate())
                    .expiryDate(command.expiryDate())
                    .issuingAuthority(command.issuingAuthority())
                    .verificationStatus(VerificationStatus.PENDING)
                    .build();

            IdentificationDocument savedDoc = identificationDocumentRepository.save(document);
            property.getDocuments().add(savedDoc);
            propertyRepository.save(property);
            return savedDoc;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to upload property document", e);
        }
    }

    @Override
    public java.util.List<IdentificationDocument> getPropertyDocuments(UUID propertyId) {
        getProperty(propertyId);
        return identificationDocumentRepository.findAllByPropertyId(propertyId);
    }

    @Override
    @Transactional
    public IdentificationDocument verifyPropertyDocument(UUID documentId, VerifyDocumentCommand command) {
        IdentificationDocument document = identificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, "Identification document not found with id: " + documentId));

        VerificationStatus targetStatus = VerificationStatus.valueOf(command.status().toUpperCase());
        document.setVerificationStatus(targetStatus);
        document.setVerifiedAt(LocalDateTime.now());
        if (targetStatus == VerificationStatus.REJECTED) {
            document.setRejectionReason(command.rejectionReason());
        } else {
            document.setRejectionReason(null);
        }

        return identificationDocumentRepository.save(document);
    }

    // Helper methods
    private Property getProperty(UUID propertyId)
    {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
    }

    private boolean isUserAdmin() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validateCompulsoryDocuments(List<UploadDocumentCommand> documentMetadata, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        List<DocumentType> compulsoryDocTypes = documentTypeRepository.findAllList().stream()
                .filter(dt -> dt.getIsCompulsory() != null && dt.getIsCompulsory())
                .toList();
        if (compulsoryDocTypes.isEmpty()) {
            return;
        }

        java.util.Set<UUID> providedDocTypeIds = new java.util.HashSet<>();
        if (documentMetadata != null) {
            for (UploadDocumentCommand info : documentMetadata) {
                if (info.documentTypeId() != null) {
                    providedDocTypeIds.add(info.documentTypeId());
                }
            }
        }

        List<String> missingDocTypes = compulsoryDocTypes.stream()
                .filter(dt -> !providedDocTypeIds.contains(dt.getId()))
                .map(DocumentType::getName)
                .toList();

        if (!missingDocTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing compulsory documents: " + String.join(", ", missingDocTypes) +
                            ". Please upload all required identification documents.");
        }
    }

    private void validatePropertyHasCompulsoryDocuments(Property property, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        List<DocumentType> compulsoryDocTypes = documentTypeRepository.findAllList().stream()
                .filter(dt -> dt.getIsCompulsory() != null && dt.getIsCompulsory())
                .toList();
        if (compulsoryDocTypes.isEmpty()) {
            return;
        }

        java.util.Set<UUID> existingDocTypeIds = property.getDocuments().stream()
                .map(doc -> doc.getDocumentType().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<String> missingDocTypes = compulsoryDocTypes.stream()
                .filter(dt -> !existingDocTypeIds.contains(dt.getId()))
                .map(DocumentType::getName)
                .toList();

        if (!missingDocTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Property is missing compulsory documents: " + String.join(", ", missingDocTypes) +
                            ". Cannot update property without all required identification documents.");
        }
    }

    private void addMediaFiles(Property property, MultipartFile[] mediaFiles) {
        if (mediaFiles == null || mediaFiles.length == 0) {
            return;
        }
        ensureMediaCollection(property);
        for (MultipartFile file : mediaFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                byte[] bytes = file.getBytes();
                String fileUrl = fileStoragePort.uploadFile(bytes, "properties/" + property.getId() + "/images", file.getOriginalFilename());
                String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

                MediaType type = MediaType.IMAGE;
                if (mimeType.startsWith("video/")) {
                    type = MediaType.VIDEO;
                } else if (mimeType.startsWith("application/") || mimeType.startsWith("text/")) {
                    type = MediaType.DOCUMENT;
                }

                Media media = Media.builder()
                        .property(property)
                        .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getName())
                        .filePath(fileUrl)
                        .mimeType(mimeType)
                        .mediaType(type)
                        .build();
                property.getMediaList().add(media);
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Failed to upload property media", e);
            }
        }
    }

    private void addDocumentFiles(Property property, MultipartFile[] documents, List<UploadDocumentCommand> documentMetadata) {
        if (documents == null || documents.length == 0) {
            return;
        }
        ensureDocumentCollection(property);

        java.util.Map<Integer, UploadDocumentCommand> metadataMap = new java.util.HashMap<>();
        if (documentMetadata != null) {
            for (UploadDocumentCommand info : documentMetadata) {
                if (info.fileIndex() != null) {
                    metadataMap.put(info.fileIndex(), info);
                }
            }
        }

        DocumentType defaultDocumentType = getOrCreateDefaultDocumentType();

        for (int i = 0; i < documents.length; i++) {
            MultipartFile documentFile = documents[i];
            if (documentFile == null || documentFile.isEmpty()) {
                continue;
            }
            try {
                byte[] bytes = documentFile.getBytes();
                String fileUrl = fileStoragePort.uploadFile(bytes, "properties/" + property.getId() + "/documents", documentFile.getOriginalFilename());
                String mimeType = documentFile.getContentType() != null ? documentFile.getContentType() : "application/octet-stream";

                UploadDocumentCommand metadata = metadataMap.get(i);

                DocumentType documentType;
                if (metadata != null && metadata.documentTypeId() != null) {
                    documentType = documentTypeRepository.findById(metadata.documentTypeId())
                            .orElseThrow(() -> new BusinessException(MSG18.CODE, "Document type not found with id: " + metadata.documentTypeId()));
                } else {
                    documentType = defaultDocumentType;
                }

                String documentNumber = (metadata != null && metadata.documentNumber() != null && !metadata.documentNumber().isBlank())
                        ? metadata.documentNumber().substring(0, Math.min(metadata.documentNumber().length(), 20))
                        : "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                String documentName = (metadata != null && metadata.documentName() != null && !metadata.documentName().isBlank())
                        ? metadata.documentName()
                        : (documentFile.getOriginalFilename() != null ? documentFile.getOriginalFilename() : documentFile.getName());

                IdentificationDocument document = IdentificationDocument.builder()
                        .documentType(documentType)
                        .property(property)
                        .documentNumber(documentNumber)
                        .documentName(documentName)
                        .filePath(fileUrl)
                        .mimeType(mimeType)
                        .issueDate(metadata != null ? metadata.issueDate() : null)
                        .expiryDate(metadata != null ? metadata.expiryDate() : null)
                        .issuingAuthority(metadata != null ? metadata.issuingAuthority() : null)
                        .verificationStatus(VerificationStatus.PENDING)
                        .build();

                property.getDocuments().add(document);
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Failed to upload property document", e);
            }
        }
    }

    private DocumentType getOrCreateDefaultDocumentType() {
        List<DocumentType> nonCompulsoryTypes = documentTypeRepository.findAllList().stream()
                .filter(dt -> dt.getIsCompulsory() != null && !dt.getIsCompulsory())
                .toList();

        if (!nonCompulsoryTypes.isEmpty()) {
            return nonCompulsoryTypes.get(0);
        }

        DocumentType defaultType = DocumentType.builder()
                .name("General Property Documents")
                .description("General documents related to property listings")
                .isCompulsory(false)
                .build();

        return documentTypeRepository.save(defaultType);
    }

    private void ensureMediaCollection(Property property) {
        if (property.getMediaList() == null) {
            property.setMediaList(new java.util.ArrayList<>());
        }
    }

    private void ensureDocumentCollection(Property property) {
        if (property.getDocuments() == null) {
            property.setDocuments(new java.util.ArrayList<>());
        }
    }

    private void removeMediaFiles(Property property, List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty() || property.getMediaList() == null || property.getMediaList().isEmpty()) {
            return;
        }

        java.util.Iterator<Media> iterator = property.getMediaList().iterator();
        while (iterator.hasNext()) {
            Media media = iterator.next();
            if (mediaIds.contains(media.getId())) {
                fileStoragePort.deleteFile(media.getFilePath());
                iterator.remove();
            }
        }
    }

    private void removeDocumentFiles(Property property, List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty() || property.getDocuments() == null || property.getDocuments().isEmpty()) {
            return;
        }

        java.util.Iterator<IdentificationDocument> iterator = property.getDocuments().iterator();
        while (iterator.hasNext()) {
            IdentificationDocument document = iterator.next();
            if (documentIds.contains(document.getId())) {
                fileStoragePort.deleteFile(document.getFilePath());
                iterator.remove();
            }
        }
    }

}
