package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum MediaTypeEnum {
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    DOCUMENT("DOCUMENT");

    private final String value;

    public static MediaTypeEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(MediaTypeEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid media type name: %s", name)));
    }
}
