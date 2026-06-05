package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ViolationTypeEnum {
    FRAUDULENT_LISTING("FRAUDULENT_LISTING"),
    MISREPRESENTATION_OF_PROPERTY("MISREPRESENTATION_OF_PROPERTY"),
    SPAM_OR_DUPLICATE_LISTING("SPAM_OR_DUPLICATE_LISTING"),
    INAPPROPRIATE_CONTENT("INAPPROPRIATE_CONTENT"),
    NON_COMPLIANCE_WITH_TERMS("NON_COMPLIANCE_WITH_TERMS"),
    FAILURE_TO_DISCLOSE_INFORMATION("FAILURE_TO_DISCLOSE_INFORMATION"),
    HARASSMENT("HARASSMENT"),
    SCAM_ATTEMPT("SCAM_ATTEMPT");

    private final String value;

    public static ViolationTypeEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(ViolationTypeEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation type name: %s", name)));
    }
}
