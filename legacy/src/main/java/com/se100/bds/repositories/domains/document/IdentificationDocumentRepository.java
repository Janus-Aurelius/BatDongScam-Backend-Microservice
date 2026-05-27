package com.se100.bds.repositories.domains.document;

import com.se100.bds.models.entities.document.IdentificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IdentificationDocumentRepository extends JpaRepository<IdentificationDocument, UUID>, JpaSpecificationExecutor<IdentificationDocument> {
}

