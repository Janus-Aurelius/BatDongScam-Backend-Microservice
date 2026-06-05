package com.se361.iam_service.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.logging.Filter;
import java.util.stream.Stream;

public final class Constants {

    @Getter
    @AllArgsConstructor
    public enum RoleEnum {
        ADMIN("ADMIN"),
        SALESAGENT("SALESAGENT"),
        PROPERTY_OWNER("PROPERTY_OWNER"),
        CUSTOMER("CUSTOMER");

        private final String value;

        public static RoleEnum get(String name){
            return Stream.of(RoleEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum StatusProfileEnum{
        ACTIVE("ACTIVE"),
        SUSPENDED("SUSPENDED"),
        PENDING_APPROVAL("PENDING_APPROVAL"),
        DELETED("DELETED"),
        REJECTED("REJECTED");

        private final String value;
        public static StatusProfileEnum get(final String name){
            return Stream.of(StatusProfileEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid Status name: %s", name)));
        }
    }

}
