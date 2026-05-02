package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.document.DocumentType;
import com.se100.bds.models.entities.document.IdentificationDocument;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.repositories.domains.document.DocumentTypeRepository;
import com.se100.bds.repositories.domains.document.IdentificationDocumentRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentDummyData {

    private final DocumentTypeRepository documentTypeRepository;
    private final IdentificationDocumentRepository identificationDocumentRepository;
    private final PropertyRepository propertyRepository;
    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    private final List<String> documentUrls = List.of(
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766917874/pexels-pixabay-261679_e8ktx7.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766917873/pexels-cytonn-955394_a7nivi.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766917872/contract_mc8xl9.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766917872/pexels-pixabay-48148_ujtscb.jpg",
            "https://res.cloudinary.com/dzpv3mfjt/image/upload/v1766917871/document-428331_1280_t8go55.jpg"
    );

    private String getRandomDocumentUrl() {
        int randomIndex = random.nextInt(documentUrls.size());
        return documentUrls.get(randomIndex);
    }

    public void createDummy() {
        createDummyDocumentTypes();
        createDummyIdentificationDocuments();
    }

    private void createDummyDocumentTypes() {
        log.info("Creating dummy document types");

        List<DocumentType> documentTypes = new ArrayList<>();

        documentTypes.add(DocumentType.builder()
                .name("Property Title Deed")
                .description("Legal document proving ownership of the property")
                .isCompulsory(true)
                .documents(new ArrayList<>())
                .build());

        documentTypes.add(DocumentType.builder()
                .name("Land Use Certificate")
                .description("Certificate of land use rights")
                .isCompulsory(true)
                .documents(new ArrayList<>())
                .build());

        documentTypes.add(DocumentType.builder()
                .name("Building Permit")
                .description("Permit for construction and building modifications")
                .isCompulsory(false)
                .documents(new ArrayList<>())
                .build());

        documentTypes.add(DocumentType.builder()
                .name("Tax Payment Certificate")
                .description("Proof of property tax payment")
                .isCompulsory(true)
                .documents(new ArrayList<>())
                .build());

        documentTypes.add(DocumentType.builder()
                .name("Owner ID Card")
                .description("National identification card of property owner")
                .isCompulsory(true)
                .documents(new ArrayList<>())
                .build());

        documentTypes.add(DocumentType.builder()
                .name("Household Registration")
                .description("Household registration book")
                .isCompulsory(false)
                .documents(new ArrayList<>())
                .build());

        documentTypeRepository.saveAll(documentTypes);
        log.info("Saved {} document types to database", documentTypes.size());
    }

    private void createDummyIdentificationDocuments() {
        log.info("Creating dummy identification documents");

        List<DocumentType> documentTypes = documentTypeRepository.findAll();
        if (documentTypes.isEmpty()) {
            log.warn("Cannot create identification documents - no document types found");
            return;
        }

        List<Property> properties = propertyRepository.findAll();
        if (properties.isEmpty()) {
            log.warn("Cannot create identification documents - no properties found");
            return;
        }

        List<IdentificationDocument> documents = new ArrayList<>();

        // Create 2-4 documents for each property
        for (Property property : properties) {
            int docCount = 2 + random.nextInt(3);

            for (int i = 0; i < docCount && i < documentTypes.size(); i++) {
                DocumentType docType = documentTypes.get(i);

                Constants.VerificationStatusEnum status = random.nextDouble() < 0.8
                        ? Constants.VerificationStatusEnum.VERIFIED
                        : Constants.VerificationStatusEnum.PENDING;

                LocalDateTime createdAt = timeGenerator.getRandomTimeAfter(property.getCreatedAt().isBefore(LocalDateTime.now()) ? property.getCreatedAt() : LocalDateTime.now().minusDays(1), LocalDateTime.now());
                LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());
                LocalDateTime verifiedAt = status == Constants.VerificationStatusEnum.VERIFIED
                        ? timeGenerator.getRandomTimeAfter(updatedAt, LocalDateTime.now())
                        : null;

                IdentificationDocument document = IdentificationDocument.builder()
                        .documentType(docType)
                        .property(property)
                        .documentNumber(String.format("DOC%09d", random.nextInt(999999999)))
                        .documentName(docType.getName() + " - " + property.getTitle())
                        .filePath(getRandomDocumentUrl())
                        .issueDate(LocalDate.now().minusYears(random.nextInt(10)))
                        .expiryDate(LocalDate.now().plusYears(5 + random.nextInt(10)))
                        .issuingAuthority(getRandomAuthority())
                        .verificationStatus(status)
                        .verifiedAt(verifiedAt)
                        .rejectionReason(status == Constants.VerificationStatusEnum.REJECTED
                                ? "Document illegible or incomplete"
                                : null)
                        .build();

                document.setCreatedAt(createdAt);
                document.setUpdatedAt(updatedAt);
                documents.add(document);
            }
        }

        identificationDocumentRepository.saveAll(documents);
        log.info("Saved {} identification documents to database", documents.size());
    }

    private String getRandomAuthority() {
        String[] authorities = {
                "Ho Chi Minh City People's Committee",
                "Hanoi People's Committee",
                "Da Nang People's Committee",
                "Ministry of Natural Resources and Environment",
                "Department of Construction",
                "Land Registration Office"
        };
        return authorities[random.nextInt(authorities.length)];
    }
}
