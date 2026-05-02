package com.se100.bds.dtos.requests.document;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeUpdateRequest {
    private UUID id;
    private String name;
    private String description;
    private Boolean isCompulsory;
}
