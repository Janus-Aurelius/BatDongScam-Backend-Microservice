package com.se.bds.core.property.internal.application.command;

public record UpdateDocumentTypeCommand(
        String name,
        String description,
        Boolean isCompulsory
) {
}
