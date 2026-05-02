package com.se100.bds.models.entities.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.violation.ViolationReport;
import com.se100.bds.utils.Constants;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "media")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "media_id", nullable = false)),
})
public class Media extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = true)
    @JsonIgnore
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id", nullable = true)
    @JsonIgnore
    private ViolationReport violationReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private Constants.MediaTypeEnum mediaType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "document_type")
    private String documentType;
}
