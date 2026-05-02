package com.se100.bds.dtos.requests.property;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePropertyTypeRequest {
    private String typeName;
    private MultipartFile avatar;
    private String description;
    private Boolean isActive;
}
