package com.se.bds.core.property.internal.application.port.out;

import com.se.bds.core.property.internal.domain.model.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTypeRepository {
    DocumentType save(DocumentType documentType);
    Optional<DocumentType> findById(UUID id);
    void delete(DocumentType documentType);
    Page<DocumentType> findAll(Pageable pageable);
    List<DocumentType> findAllList();
}
