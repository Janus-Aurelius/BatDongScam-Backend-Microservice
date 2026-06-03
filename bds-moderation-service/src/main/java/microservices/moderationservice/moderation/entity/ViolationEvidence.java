package microservices.moderationservice.moderation.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import com.se.bds.common.enums.MediaTypeEnum;

@Embeddable
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ViolationEvidence {
    private String fileUrl;
    
    @Enumerated(EnumType.STRING)
    private MediaTypeEnum mediaType;
    
    private String fileName;
    private String mimeType;
}
