package com.se100.bds.dtos.requests.property;

import com.se100.bds.utils.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO containing metadata for uploading a media file.
 * Each media upload should have associated metadata to properly categorize it.
 */
@Getter
@Setter
@NoArgsConstructor
public class MediaUploadInfo {
    /**
     * The type of media (IMAGE, VIDEO, DOCUMENT).
     * If not provided, will be auto-detected from the file's MIME type.
     */
    private Constants.MediaTypeEnum mediaType;

    /**
     * Optional custom file name.
     * If not provided, will use the uploaded file's original filename.
     */
    private String fileName;

    /**
     * Optional document type description (for DOCUMENT media type).
     * E.g., "Floor Plan", "Property Photos", etc.
     */
    private String documentType;

    /**
     * Index of the corresponding file in the images/media MultipartFile array.
     * This links the metadata to the actual uploaded file.
     */
    @NotNull(message = "File index is required")
    private Integer fileIndex;
}
