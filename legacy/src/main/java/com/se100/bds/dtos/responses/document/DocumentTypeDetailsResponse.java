package com.se100.bds.dtos.responses.document;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DocumentTypeDetailsResponse extends AbstractBaseDataResponse {
    private String name;
    private String description;
    private Boolean isCompulsory;
}
