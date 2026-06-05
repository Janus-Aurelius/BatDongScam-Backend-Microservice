package com.se.bds.core.property.internal.application.command;

public record CreateDocumentTypeCommand(
        String name,
        String description,
        Boolean isCompulsory
) {
}
