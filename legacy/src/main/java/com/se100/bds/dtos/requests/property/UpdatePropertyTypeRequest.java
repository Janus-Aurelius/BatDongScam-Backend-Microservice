package com.se100.bds.dtos.requests.property;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyTypeRequest {
    private UUID id;
    private String typeName;
    private MultipartFile avatar;
    private String description;
    private Boolean isActive;
}
