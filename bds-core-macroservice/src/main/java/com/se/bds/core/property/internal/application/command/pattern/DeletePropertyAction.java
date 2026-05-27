package com.se.bds.core.property.internal.application.command.pattern;

import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.property.internal.domain.model.PropertyStatus;

public class DeletePropertyAction implements ReversibleCommand<Property> {

    private final PropertyRepository repository;
    private final Property property;
    private final PropertyStatus oldStatus;

    public DeletePropertyAction(PropertyRepository repository, Property property) {
        this.repository = repository;
        this.property = property;
        this.oldStatus = property.getStatus();
    }

    @Override
    public Property execute() {
        property.markAsDeleted();
        return repository.save(property);
    }

    @Override
    public void undo() {
        property.setStatus(oldStatus); // Khôi phục lại trạng thái trước khi xóa
        repository.save(property);
    }
}
