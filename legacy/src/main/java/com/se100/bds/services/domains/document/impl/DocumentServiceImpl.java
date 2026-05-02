package com.se100.bds.services.domains.document.impl;

import com.se100.bds.dtos.requests.document.DocumentTypeCreateRequest;
import com.se100.bds.dtos.requests.document.DocumentTypeUpdateRequest;
import com.se100.bds.dtos.responses.document.DocumentTypeDetailsResponse;
import com.se100.bds.dtos.responses.document.DocumentTypeListItemResponse;
import com.se100.bds.mappers.DocumentMapper;
import com.se100.bds.models.entities.document.DocumentType;
import com.se100.bds.repositories.domains.document.DocumentTypeRepository;
import com.se100.bds.services.domains.document.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentMapper documentMapper;

    @Override
    public Page<DocumentTypeListItemResponse> getAllWithFilter(Pageable pageable, Boolean isCompulsory) {
        if (isCompulsory == null) {
            // GET all
            return documentMapper.mapToPage(
                    documentTypeRepository.findAll(pageable),
                    DocumentTypeListItemResponse.class
            );
        } else {
            return documentMapper.mapToPage(
                    documentTypeRepository.findAllByIsCompulsory(isCompulsory, pageable),
                    DocumentTypeListItemResponse.class
            );
        }
    }

    @Override
    public DocumentTypeDetailsResponse getById(UUID id) {
        return documentMapper.mapTo(
                documentTypeRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Document type not found with id")),
                DocumentTypeDetailsResponse.class
        );
    }

    @Override
    public DocumentTypeDetailsResponse create(DocumentTypeCreateRequest documentTypeCreateRequest) {
        DocumentType newDocumentType = documentMapper.mapTo(documentTypeCreateRequest, DocumentType.class);
        return documentMapper.mapTo(
                documentTypeRepository.save(newDocumentType),
                DocumentTypeDetailsResponse.class
        );
    }

    @Override
    public DocumentTypeDetailsResponse update(DocumentTypeUpdateRequest documentTypeUpdateRequest) {
        DocumentType documentType = documentTypeRepository.findById(documentTypeUpdateRequest.getId())
                .orElseThrow(() -> new EntityNotFoundException("Document type not found with id: " + documentTypeUpdateRequest.getId()));

        // Update fields only if they are not null
        if (documentTypeUpdateRequest.getName() != null) {
            documentType.setName(documentTypeUpdateRequest.getName());
        }
        if (documentTypeUpdateRequest.getDescription() != null) {
            documentType.setDescription(documentTypeUpdateRequest.getDescription());
        }
        if (documentTypeUpdateRequest.getIsCompulsory() != null) {
            documentType.setIsCompulsory(documentTypeUpdateRequest.getIsCompulsory());
        }

        DocumentType savedDocumentType = documentTypeRepository.save(documentType);
        return documentMapper.mapTo(savedDocumentType, DocumentTypeDetailsResponse.class);
    }

    @Override
    public void delete(UUID id) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document type not found with id: " + id));
        documentTypeRepository.delete(documentType);
    }
}
