package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.domain.model.DocumentType;
import com.se.bds.core.property.internal.domain.model.IdentificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface JpaDocumentTypeRepository extends JpaRepository<DocumentType, UUID> {
}

interface JpaIdentificationDocumentRepository extends JpaRepository<IdentificationDocument, UUID> {
    @Query("SELECT d FROM IdentificationDocument d WHERE d.property.id = :propertyId")
    List<IdentificationDocument> findAllByPropertyId(@Param("propertyId") UUID propertyId);
}
