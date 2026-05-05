package com.se.bds.core.property.internal.adapter.in.web.mapper;

import com.se.bds.core.property.internal.application.command.CreatePropertyCommand;
import com.se.bds.core.property.internal.application.command.UpdatePropertyCommand;

@Mapper (componentModel = MappingConstants.ComponentModel.SPRING)
public interface PropertyWebMapper {
    CreatePropertyCommand toCreatePropertyCommand (CreatePropertyWebRequest request);
    UpdatePropertyCommand toUpdatePropertyComman (UpdatePropertyWebRequest request);
}
