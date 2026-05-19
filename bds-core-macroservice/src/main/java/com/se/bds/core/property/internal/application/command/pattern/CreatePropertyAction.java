package com.se.bds.core.property.internal.application.command.pattern;

import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.domain.model.Property;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreatePropertyAction implements ReversibleCommand<Property> {

    private final PropertyRepository repository;
    private final Property property;

    @Override
    public Property execute() {
        return repository.save(property);
    }

    @Override
    public void undo() {
        property.markAsDeleted();
        repository.save(property);
    }
}
