package com.se.bds.core.property.internal.application.command;

import java.util.UUID;

public record UpdatePropertyTypeCommand(
        String typeName,
        String description,
        Boolean isActive
) {
}
