package com.se.bds.core.property.internal.application.command.pattern;

import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.domain.model.Property;

import java.math.BigDecimal;

public class UpdatePropertyAction implements ReversibleCommand<Property> {

    private final PropertyRepository repository;
    private final Property property;
    // Lưu lại data cũ for Restoration
    private final String oldTitle;
    private final BigDecimal oldPrice;

    public UpdatePropertyAction(PropertyRepository repository, Property property) {
        this.repository = repository;
        this.property = property;
        this.oldTitle = property.getTitle();
        this.oldPrice = property.getPriceAmount();
    }

    @Override
    public Property execute() {
        return repository.save(property);
    }

    @Override
    public void undo() {
        property.setTitle(oldTitle);
        property.setPriceAmount(oldPrice);
        repository.save(property);
    }
}
