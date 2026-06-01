package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.application.port.out.DocumentTypeRepository;
import com.se.bds.core.property.internal.domain.model.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentTypeRepositoryAdapter implements DocumentTypeRepository {

    private final JpaDocumentTypeRepository jpaDocumentTypeRepository;

    @Override
    public DocumentType save(DocumentType documentType) {
        return jpaDocumentTypeRepository.save(documentType);
    }

    @Override
    public Optional<DocumentType> findById(UUID id) {
        return jpaDocumentTypeRepository.findById(id);
    }

    @Override
    public void delete(DocumentType documentType) {
        jpaDocumentTypeRepository.delete(documentType);
    }

    @Override
    public Page<DocumentType> findAll(Pageable pageable) {
        return jpaDocumentTypeRepository.findAll(pageable);
    }

    @Override
    public List<DocumentType> findAllList() {
        return jpaDocumentTypeRepository.findAll();
    }
}
