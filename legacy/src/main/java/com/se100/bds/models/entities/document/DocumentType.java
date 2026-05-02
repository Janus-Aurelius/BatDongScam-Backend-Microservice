package com.se100.bds.models.entities.document;

import com.se100.bds.models.entities.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_types")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "document_type_id", nullable = false)),
})
public class DocumentType extends AbstractBaseEntity {
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_compulsory")
    private Boolean isCompulsory;

    @OneToMany(mappedBy = "documentType", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IdentificationDocument> documents = new ArrayList<>();
}
