package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.repositories.domains.property.PropertyTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PropertyTypeDummyData {

    private final PropertyTypeRepository propertyTypeRepository;
    private final TimeGenerator timeGenerator = new TimeGenerator();

    public void createDummy() {
        createDummyPropertyTypes();
    }

    private void createDummyPropertyTypes() {
        log.info("Creating dummy property types");

        List<PropertyType> propertyTypes = new ArrayList<>();

        propertyTypes.add(createPropertyType("Apartment", "Modern apartment buildings with multiple units"));
        propertyTypes.add(createPropertyType("House", "Standalone residential houses"));
        propertyTypes.add(createPropertyType("Villa", "Luxury villas with private gardens"));
        propertyTypes.add(createPropertyType("Townhouse", "Multi-story houses in urban areas"));
        propertyTypes.add(createPropertyType("Penthouse", "Luxury apartments on top floors"));
        propertyTypes.add(createPropertyType("Studio", "Compact single-room apartments"));
        propertyTypes.add(createPropertyType("Duplex", "Two-floor apartments"));
        propertyTypes.add(createPropertyType("Office Space", "Commercial office properties"));
        propertyTypes.add(createPropertyType("Shop House", "Mixed-use commercial and residential"));
        propertyTypes.add(createPropertyType("Land", "Vacant land plots"));

        propertyTypeRepository.saveAll(propertyTypes);
        log.info("Saved {} property types to database", propertyTypes.size());
    }

    private PropertyType createPropertyType(String typeName, String description) {
        LocalDateTime createdAt = timeGenerator.getRandomTime();
        LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());

        PropertyType propertyType = PropertyType.builder()
                .typeName(typeName)
                .description(description)
                .avatarUrl(null)
                .isActive(true)
                .properties(new ArrayList<>())
                .build();

        propertyType.setCreatedAt(createdAt);
        propertyType.setUpdatedAt(updatedAt);
        return propertyType;
    }
}
