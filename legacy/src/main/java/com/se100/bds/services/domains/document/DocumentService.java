package com.se100.bds.services.domains.document;

import com.se100.bds.dtos.requests.document.DocumentTypeCreateRequest;
import com.se100.bds.dtos.requests.document.DocumentTypeUpdateRequest;
import com.se100.bds.dtos.responses.document.DocumentTypeDetailsResponse;
import com.se100.bds.dtos.responses.document.DocumentTypeListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentService {
    Page<DocumentTypeListItemResponse> getAllWithFilter(Pageable pageable, Boolean isCompulsory);
    DocumentTypeDetailsResponse getById(UUID id);
    DocumentTypeDetailsResponse create(DocumentTypeCreateRequest documentTypeCreateRequest);
    DocumentTypeDetailsResponse update(DocumentTypeUpdateRequest documentTypeUpdateRequest);
    void delete(UUID id);
}
