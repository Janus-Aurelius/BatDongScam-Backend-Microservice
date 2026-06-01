package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.internal.domain.model.IdentificationDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentificationDocumentRepository {
    IdentificationDocument save(IdentificationDocument document);
    Optional<IdentificationDocument> findById(UUID id);
    void delete(IdentificationDocument document);
    List<IdentificationDocument> findAllByPropertyId(UUID propertyId);
}
