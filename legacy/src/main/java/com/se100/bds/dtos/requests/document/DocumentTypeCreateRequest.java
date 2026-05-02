package com.se100.bds.dtos.requests.document;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeCreateRequest {
    private String name;
    private String description;
    private Boolean isCompulsory;
}
