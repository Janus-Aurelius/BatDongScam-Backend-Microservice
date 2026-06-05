package com.se.bds.search.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

public final class Constants {

    @Getter
    @AllArgsConstructor
    public enum SearchTypeEnum {
        PROPERTY("PROPERTY"),
        CITY("CITY"),
        DISTRICT("DISTRICT"),
        WARD("WARD"),
        PROPERTY_TYPE("PROPERTY_TYPE");

        private final String value;

        public static SearchTypeEnum get(final String name) {
            return Stream.of(SearchTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid search type name: %s", name)));
        }
    }
}
