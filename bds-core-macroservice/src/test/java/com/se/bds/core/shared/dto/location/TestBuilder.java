package com.se.bds.core.shared.dto.location;

import java.util.UUID;

public class TestBuilder {
    public void test() {
        LocationCardResponse response = LocationCardResponse.builder()
            .id(UUID.randomUUID())
            .name("Test")
            .build();
    }
}
