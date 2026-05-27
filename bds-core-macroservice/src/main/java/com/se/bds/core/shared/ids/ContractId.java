package com.se.bds.core.shared.ids;

import java.util.UUID;

public record ContractId(UUID value) {
    public static ContractId of (UUID value) {
        return new ContractId(value);
    }
}
