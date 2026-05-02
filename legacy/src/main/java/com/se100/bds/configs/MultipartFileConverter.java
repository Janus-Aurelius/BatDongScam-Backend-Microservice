package com.se100.bds.configs;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MultipartFileConverter implements Converter<String, MultipartFile> {
    @Override
    public MultipartFile convert(String source) {
        // Convert empty string to null to avoid conversion errors
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        // For non-empty strings, throw exception as we can't convert arbitrary strings to files
        throw new IllegalArgumentException("Cannot convert non-empty string to MultipartFile");
    }
}

