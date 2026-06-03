package com.se.bds.core.property.internal.application.port.in;

import com.se.bds.core.property.internal.application.command.*;
import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.property.internal.domain.model.PropertyType;
import com.se.bds.core.property.internal.domain.model.DocumentType;
import com.se.bds.core.property.internal.domain.model.IdentificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PropertyUseCase {
    //core mutations
    Property createProperty(CreatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents);
    Property updateProperty(UUID propertyId,UpdatePropertyCommand command, MultipartFile[] mediaFiles, MultipartFile[] documents);
    Property updatePropertyStatus(UUID propertyId, UpdatePropertyStatusCommand command);
    void deleteProperty(UUID propertyId);
    void assignAgent(UUID propertyId, UUID agentId);

    //query
    Page<Property> searchProperties(SearchPropertyCommand command, Pageable pageable);
    Property getPropertyDetail(UUID propertyId);

    //property type
    PropertyType createPropertyType(CreatePropertyTypeCommand command);
    PropertyType updatePropertyType(UUID id, UpdatePropertyTypeCommand command);
    void deletePropertyType(UUID id);
    Page<PropertyType> getAllPropertyTypes(Pageable pageable);
    java.util.List<UUID> getAllAvailablePropertyTypeIds();
    String getPropertyTypeName(UUID propertyTypeId);
    int countPropertiesByPropertyTypeId(UUID propertyTypeId);

    //document type CRUD
    DocumentType createDocumentType(CreateDocumentTypeCommand command);
    DocumentType updateDocumentType(UUID id, UpdateDocumentTypeCommand command);
    void deleteDocumentType(UUID id);
    Page<DocumentType> getAllDocumentTypes(Pageable pageable);
    java.util.List<DocumentType> getAllDocumentTypesList();

    //property documents
    IdentificationDocument uploadPropertyDocument(UUID propertyId, UploadDocumentCommand command, MultipartFile file);
    java.util.List<IdentificationDocument> getPropertyDocuments(UUID propertyId);
    IdentificationDocument verifyPropertyDocument(UUID documentId, VerifyDocumentCommand command);

    PropertyLocationInfo getPropertyLocationInfo(UUID propertyId);

    record PropertyLocationInfo(
            UUID propertyId,
            UUID cityId,
            UUID districtId,
            UUID wardId,
            UUID propertyTypeId
    ) {}
}
