package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.document.DocumentTypeCreateRequest;
import com.se100.bds.dtos.requests.document.DocumentTypeUpdateRequest;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.SuccessResponse;
import com.se100.bds.dtos.responses.document.DocumentTypeDetailsResponse;
import com.se100.bds.dtos.responses.error.DetailedErrorResponse;
import com.se100.bds.dtos.responses.error.ErrorResponse;
import com.se100.bds.services.domains.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("documents")
@Tag(name = "008. Documents", description = "Document Type Management API")
@Slf4j
public class DocumentController extends AbstractBaseController {
    private final DocumentService documentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(
            summary = "Create a new document type",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            description = "Create a new document type with name, description, and compulsory flag",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Document type created successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DetailedErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<DocumentTypeDetailsResponse>> createDocumentType(
            @Parameter(description = "Request body to create document type", required = true)
            @Valid @RequestBody DocumentTypeCreateRequest request
    ) {
        log.info("Creating new document type - name: {}", request.getName());
        DocumentTypeDetailsResponse response = documentService.create(request);
        return responseFactory.successSingle(response, "Document type created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping
    @Operation(
            summary = "Update an existing document type",
            description = "Update document type details including name, description, and compulsory flag",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Document type updated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DetailedErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Document type not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<DocumentTypeDetailsResponse>> updateDocumentType(
            @Parameter(description = "Request body to update document type", required = true)
            @Valid @RequestBody DocumentTypeUpdateRequest request
    ) {
        log.info("Updating document type - id: {}", request.getId());
        DocumentTypeDetailsResponse response = documentService.update(request);
        return responseFactory.successSingle(response, "Document type updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a document type by ID",
            description = "Delete a document type by ID. This will also delete all associated documents.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Document type deleted successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Document type not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<SuccessResponse>> deleteDocumentType(
            @Parameter(description = "Document type ID to delete", required = true)
            @PathVariable UUID id
    ) {
        log.info("Deleting document type - id: {}", id);
        documentService.delete(id);
        return responseFactory.successSingle(null, "Document type deleted successfully");
    }
}

