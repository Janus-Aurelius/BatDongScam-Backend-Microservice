package com.se.bds.core.shared.ids;

import java.util.UUID;

public record PropertyId(UUID value) {
    public static PropertyId of (UUID value) {
        return new PropertyId(value);
    }
}
