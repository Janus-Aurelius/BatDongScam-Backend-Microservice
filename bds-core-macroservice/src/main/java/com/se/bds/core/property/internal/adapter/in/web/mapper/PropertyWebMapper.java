package com.se.bds.core.property.internal.adapter.in.web.mapper;

import com.se.bds.core.property.internal.adapter.in.web.dto.CreatePropertyWebRequest;
import com.se.bds.core.property.internal.adapter.in.web.dto.UpdatePropertyWebRequest;
import com.se.bds.core.property.internal.application.command.CreatePropertyCommand;
import com.se.bds.core.property.internal.application.command.UpdatePropertyCommand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PropertyWebMapper {
    CreatePropertyCommand toCreatePropertyCommand (CreatePropertyWebRequest request);
    UpdatePropertyCommand toUpdatePropertyCommand(UpdatePropertyWebRequest request);
}
