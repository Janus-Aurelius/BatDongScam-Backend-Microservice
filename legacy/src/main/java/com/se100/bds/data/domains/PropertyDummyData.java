package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.property.PropertyTypeRepository;
import com.se100.bds.repositories.domains.user.PropertyOwnerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class PropertyDummyData {

    private final PropertyRepository propertyRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final WardRepository wardRepository;

    private final Random random = new Random();
    private final TimeGenerator timeGenerator = new TimeGenerator();

    public void createDummy() {
        createDummyProperties();
    }

    private void createDummyProperties() {
        log.info("Creating dummy properties");

        List<PropertyOwner> owners = propertyOwnerRepository.findAll();
        if (owners.isEmpty()) {
            log.warn("Cannot create properties - no property owners found");
            return;
        }

        List<SaleAgent> agents = saleAgentRepository.findAll();
        if (agents.isEmpty()) {
            log.warn("Cannot create properties - no sale agents found");
            return;
        }

        List<PropertyType> types = propertyTypeRepository.findAll();
        if (types.isEmpty()) {
            log.warn("Cannot create properties - no property types found");
            return;
        }

        List<Ward> wards = wardRepository.findAll();
        if (wards.isEmpty()) {
            log.warn("Cannot create properties - no wards found");
            return;
        }

        List<Property> properties = new ArrayList<>();

        // Create 200 properties
        for (int i = 1; i <= 200; i++) {
            PropertyOwner owner = owners.get(random.nextInt(owners.size()));
            SaleAgent agent = agents.get(random.nextInt(agents.size()));
            PropertyType type = types.get(random.nextInt(types.size()));
            Ward ward = wards.get(random.nextInt(wards.size()));

            Constants.TransactionTypeEnum transactionType = random.nextBoolean()
                    ? Constants.TransactionTypeEnum.SALE
                    : Constants.TransactionTypeEnum.RENTAL;

            BigDecimal area = new BigDecimal(50 + random.nextInt(450)); // 50-500 sqm
            BigDecimal pricePerSqm = new BigDecimal(50000000 + random.nextInt(200000000)); // 50M-250M VND/sqm
            BigDecimal priceAmount = area.multiply(pricePerSqm);

            LocalDateTime createdAt = timeGenerator.getRandomTime();
            LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, null);
            LocalDateTime approvedAt = timeGenerator.getRandomTimeAfter(updatedAt, null);

            Property property = Property.builder()
                    .owner(owner)
                    .assignedAgent(agent)
                    .propertyType(type)
                    .ward(ward)
                    .title(generatePropertyTitle(type.getTypeName(), ward, i))
                    .description(generatePropertyDescription(type.getTypeName(), area))
                    .transactionType(transactionType)
                    .fullAddress(String.format("%d %s Street, %s", i, type.getTypeName(), ward.getWardName()))
                    .area(area)
                    .rooms(2 + random.nextInt(6)) // 2-7 rooms
                    .bathrooms(1 + random.nextInt(4)) // 1-4 bathrooms
                    .floors(1 + random.nextInt(5)) // 1-5 floors
                    .bedrooms(1 + random.nextInt(5)) // 1-5 bedrooms
                    .houseOrientation(getRandomOrientation())
                    .balconyOrientation(getRandomOrientation())
                    .yearBuilt(2000 + random.nextInt(25)) // 2000-2024
                    .priceAmount(priceAmount)
                    .serviceFeeAmount(priceAmount.multiply(BigDecimal.valueOf(0.02)))
                    .pricePerSquareMeter(pricePerSqm)
                    .commissionRate(new BigDecimal("0.02")) // 2% commission
                    .amenities("WiFi, Air Conditioning, Parking, Security")
                    .serviceFeeCollectedAmount(random.nextDouble() < 0.8 ? priceAmount.multiply(new BigDecimal("0.02")) : BigDecimal.ZERO)
                    .status(random.nextDouble() < 0.8 ? Constants.PropertyStatusEnum.AVAILABLE : Constants.PropertyStatusEnum.PENDING)
                    .viewCount(random.nextInt(1000))
                    .approvedAt(approvedAt)
                    .mediaList(new ArrayList<>())
                    .appointments(new ArrayList<>())
                    .contracts(new ArrayList<>())
                    .documents(new ArrayList<>())
                    .build();

            property.setCreatedAt(createdAt);
            property.setUpdatedAt(updatedAt);

            properties.add(property);
        }

        propertyRepository.saveAll(properties);
        log.info("Saved {} properties to database", properties.size());
    }

    private String generatePropertyTitle(String typeName, Ward ward, int index) {
        return String.format("Beautiful %s in %s - Property #%d", typeName, ward.getWardName(), index);
    }

    private String generatePropertyDescription(String typeName, BigDecimal area) {
        return String.format("Modern %s with %.2f square meters of living space. " +
                "Located in a prime location with easy access to amenities. " +
                "Perfect for families or investors looking for quality property.",
                typeName.toLowerCase(), area);
    }

    private Constants.OrientationEnum getRandomOrientation() {
        Constants.OrientationEnum[] orientations = Constants.OrientationEnum.values();
        return orientations[random.nextInt(orientations.length)];
    }
}
