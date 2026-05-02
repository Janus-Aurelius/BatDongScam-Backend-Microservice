package com.se100.bds.mappers;

import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public abstract class BaseMapper {

    protected final ModelMapper modelMapper;

    @Autowired
    protected BaseMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.registerCommonConverters();
        this.configureCustomMappings();
    }

    private void registerCommonConverters() {
        // UUID -> String
        modelMapper.addConverter(new AbstractConverter<UUID, String>() {
            @Override
            protected String convert(UUID source) {
                return source != null ? source.toString() : null;
            }
        });

        // Enum -> String
        modelMapper.addConverter(new AbstractConverter<Enum<?>, String>() {
            @Override
            protected String convert(Enum<?> source) {
                return source != null ? source.name() : null;
            }
        });

        // LocalDateTime -> String
        modelMapper.addConverter(new AbstractConverter<LocalDateTime, String>() {
            @Override
            protected String convert(LocalDateTime source) {
                if (source == null) return null;
                return source.format(DateTimeFormatter.ISO_DATE_TIME);
            }
        });
    }

    protected abstract void configureCustomMappings();

    public <T> T mapTo(Object source, Class<T> targetType) {
        if (source == null) return null;
        return modelMapper.map(source, targetType);
    }

    public <T> List<T> mapToList(List<?> sourceList, Class<T> targetType) {
        if (sourceList == null || sourceList.isEmpty()) return List.of();
        return sourceList.stream()
                .map(source -> mapTo(source, targetType))
                .collect(Collectors.toList());
    }

    public <T> Page<T> mapToPage(Page<?> sourcePage, Class<T> targetType) {
        if (sourcePage == null) return Page.empty();
        return sourcePage.map(source -> mapTo(source, targetType));
    }
}