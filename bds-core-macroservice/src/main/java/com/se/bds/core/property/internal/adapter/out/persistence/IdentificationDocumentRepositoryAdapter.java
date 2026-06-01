package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.application.port.out.IdentificationDocumentRepository;
import com.se.bds.core.property.internal.domain.model.IdentificationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdentificationDocumentRepositoryAdapter implements IdentificationDocumentRepository {

    private final JpaIdentificationDocumentRepository jpaIdentificationDocumentRepository;

    @Override
    public IdentificationDocument save(IdentificationDocument document) {
        return jpaIdentificationDocumentRepository.save(document);
    }

    @Override
    public Optional<IdentificationDocument> findById(UUID id) {
        return jpaIdentificationDocumentRepository.findById(id);
    }

    @Override
    public void delete(IdentificationDocument document) {
        jpaIdentificationDocumentRepository.delete(document);
    }

    @Override
    public List<IdentificationDocument> findAllByPropertyId(UUID propertyId) {
        return jpaIdentificationDocumentRepository.findAllByPropertyId(propertyId);
    }
}
