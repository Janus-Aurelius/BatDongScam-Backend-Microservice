package com.se.bds.core.property.internal.application.command;

public record CreatePropertyTypeCommand(
        String typeName,
        String description,
        Boolean isActive
) {
}
