package com.se.bds.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

//published when a contract is fully signed
//listener: property module (update property status), financial service (payment schedules)

public record ContractSignedEvent (
    UUID contractId,
    UUID propertyId,
    String contractType,
    LocalDateTime signedAt
){}
