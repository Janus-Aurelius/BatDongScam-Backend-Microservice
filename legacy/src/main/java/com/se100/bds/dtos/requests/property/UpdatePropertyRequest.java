package com.se100.bds.dtos.requests.property;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePropertyRequest extends CreatePropertyRequest {
    @Size(max = 50, message = "Cannot remove more than 50 media items in a single request")
    private List<UUID> mediaIdsToRemove;

    @Size(max = 50, message = "Cannot remove more than 50 document items in a single request")
    private List<UUID> documentIdsToRemove;
}
