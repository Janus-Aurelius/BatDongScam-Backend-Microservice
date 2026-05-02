package com.se100.bds.dtos.responses.property;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class MediaResponse extends AbstractBaseDataResponse {
    private String mediaType;
    private String fileName;
    private String filePath;
    private String mimeType;
}

