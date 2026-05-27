package com.se100.bds.mappers;

import com.se100.bds.dtos.responses.document.DocumentResponse;
import com.se100.bds.dtos.responses.property.MediaResponse;
import com.se100.bds.dtos.responses.property.PropertyDetails;
import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.dtos.responses.user.simple.SimpleUserResponse;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.repositories.dtos.DocumentProjection;
import com.se100.bds.repositories.dtos.MediaProjection;
import com.se100.bds.repositories.dtos.PropertyDetailsProjection;
import com.se100.bds.services.domains.ranking.RankingService;
import com.se100.bds.services.dtos.results.PropertyCard;
import com.se100.bds.utils.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PropertyMapper extends BaseMapper {

    private final RankingService rankingService;

    @Autowired
    public PropertyMapper(ModelMapper modelMapper, RankingService rankingService) {
        super(modelMapper);
        this.rankingService = rankingService;
    }

    @Override
    protected void configureCustomMappings() {
        // Existing mapping for PropertyCard to SimplePropertyCard
        modelMapper.typeMap(PropertyCard.class, SimplePropertyCard.class)
                .addMappings(mapper -> {
                    mapper.using(ctx -> {
                        PropertyCard propertyCard = (PropertyCard) ctx.getSource();
                        String district = propertyCard.getDistrict();
                        String city = propertyCard.getCity();

                        if (district != null && city != null) {
                            return district + ", " + city;
                        } else if (city != null) {
                            return city;
                        }
                        return district;
                    }).map(src -> src, (dest, value) -> dest.setLocation((String) value));

                    // Map transactionType from String to Enum
                    mapper.using(ctx -> {
                        PropertyCard propertyCard = (PropertyCard) ctx.getSource();
                        String transactionTypeStr = propertyCard.getTransactionType();
                        if (transactionTypeStr != null) {
                            try {
                                return Constants.TransactionTypeEnum.valueOf(transactionTypeStr);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        }
                        return null;
                    }).map(src -> src, (dest, value) -> dest.setTransactionType((Constants.TransactionTypeEnum) value));
                });

        // Mapping for Property entity to SimplePropertyCard
        modelMapper.typeMap(Property.class, SimplePropertyCard.class)
                .addMappings(mapper -> {
                    // Map basic fields
                    mapper.map(Property::getTitle, SimplePropertyCard::setTitle);
                    mapper.map(Property::getTransactionType, SimplePropertyCard::setTransactionType);
                    mapper.map(Property::getPriceAmount, SimplePropertyCard::setPrice);
                    mapper.map(Property::getArea, SimplePropertyCard::setTotalArea);

                    // Map status
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getStatus() != null ? property.getStatus().name() : null;
                    }).map(src -> src, SimplePropertyCard::setStatus);

                    // Map thumbnail from media list
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        if (property.getMediaList() != null && !property.getMediaList().isEmpty()) {
                            return property.getMediaList().get(0).getFilePath();
                        }
                        return null;
                    }).map(src -> src, SimplePropertyCard::setThumbnailUrl);

                    // Map number of images
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getMediaList() != null ? property.getMediaList().size() : 0;
                    }).map(src -> src, SimplePropertyCard::setNumberOfImages);

                    // Map location (district, city)
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        if (property.getWard() != null && property.getWard().getDistrict() != null) {
                            String district = property.getWard().getDistrict().getDistrictName();
                            String city = property.getWard().getDistrict().getCity() != null
                                    ? property.getWard().getDistrict().getCity().getCityName() : null;

                            if (district != null && city != null) {
                                return district + ", " + city;
                            } else if (city != null) {
                                return city;
                            }
                            return district;
                        }
                        return null;
                    }).map(src -> src, SimplePropertyCard::setLocation);

                    // Map owner fields
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getOwner() != null ? property.getOwner().getId() : null;
                    }).map(src -> src, SimplePropertyCard::setOwnerId);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getOwner() != null && property.getOwner().getUser() != null
                                ? property.getOwner().getUser().getFirstName() : null;
                    }).map(src -> src, SimplePropertyCard::setOwnerFirstName);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getOwner() != null && property.getOwner().getUser() != null
                                ? property.getOwner().getUser().getLastName() : null;
                    }).map(src -> src, SimplePropertyCard::setOwnerLastName);

                    // Map agent fields
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getAssignedAgent() != null ? property.getAssignedAgent().getId() : null;
                    }).map(src -> src, SimplePropertyCard::setAgentId);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getAssignedAgent() != null && property.getAssignedAgent().getUser() != null
                                ? property.getAssignedAgent().getUser().getFirstName() : null;
                    }).map(src -> src, SimplePropertyCard::setAgentFirstName);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getAssignedAgent() != null && property.getAssignedAgent().getUser() != null
                                ? property.getAssignedAgent().getUser().getLastName() : null;
                    }).map(src -> src, SimplePropertyCard::setAgentLastName);

                    // isFavorite will be set to false by default (requires additional logic)
                    mapper.skip(SimplePropertyCard::setFavorite);

                    // Tiers will be set separately in the service layer
                    mapper.skip(SimplePropertyCard::setOwnerTier);
                    mapper.skip(SimplePropertyCard::setAgentTier);
                });

        // Custom mapping for Property to PropertyDetails
        modelMapper.typeMap(Property.class, PropertyDetails.class)
                .addMappings(mapper -> {
                    // I dont know why this shit doesnt work
                    mapper.map(Property::getCreatedAt, PropertyDetails::setCreatedAt);
                    mapper.map(Property::getUpdatedAt, PropertyDetails::setUpdatedAt);

                    // Map owner
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        if (property.getOwner() != null && property.getOwner().getUser() != null) {
                            var user = property.getOwner().getUser();
                            SimpleUserResponse simpleUserResponse = modelMapper.map(user, SimpleUserResponse.class);
                            simpleUserResponse.setTier(rankingService.getCurrentTier(user.getId(), user.getRole()));
                            return simpleUserResponse;
                        }
                        return null;
                    }).map(src -> src, PropertyDetails::setOwner);

                    // Map assigned agent
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        if (property.getAssignedAgent() != null && property.getAssignedAgent().getUser() != null) {
                            var user = property.getAssignedAgent().getUser();
                            return SimpleUserResponse.builder()
                                    .id(user.getId())
                                    .firstName(user.getFirstName())
                                    .lastName(user.getLastName())
                                    .phoneNumber(user.getPhoneNumber())
                                    .createdAt(user.getCreatedAt())
                                    .updatedAt(user.getUpdatedAt())
                                    .build();
                        }
                        return null;
                    }).map(src -> src, PropertyDetails::setAssignedAgent);

                    // Map property type
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getPropertyType() != null ? property.getPropertyType().getId() : null;
                    }).map(src -> src, PropertyDetails::setPropertyTypeId);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getPropertyType() != null ? property.getPropertyType().getTypeName() : null;
                    }).map(src -> src, PropertyDetails::setPropertyTypeName);

                    // Map location hierarchy
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getWard() != null ? property.getWard().getId() : null;
                    }).map(src -> src, PropertyDetails::setWardId);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getWard() != null ? property.getWard().getWardName() : null;
                    }).map(src -> src, PropertyDetails::setWardName);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getWard() != null && property.getWard().getDistrict() != null
                                ? property.getWard().getDistrict().getId() : null;
                    }).map(src -> src, PropertyDetails::setDistrictId);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getWard() != null && property.getWard().getDistrict() != null
                                ? property.getWard().getDistrict().getDistrictName() : null;
                    }).map(src -> src, PropertyDetails::setDistrictName);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getWard() != null && property.getWard().getDistrict() != null
                                && property.getWard().getDistrict().getCity() != null
                                ? property.getWard().getDistrict().getCity().getId() : null;
                    }).map(src -> src, PropertyDetails::setCityId);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getWard() != null && property.getWard().getDistrict() != null
                                && property.getWard().getDistrict().getCity() != null
                                ? property.getWard().getDistrict().getCity().getCityName() : null;
                    }).map(src -> src, PropertyDetails::setCityName);

                    // Map enums to strings
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getTransactionType() != null ? property.getTransactionType().name() : null;
                    }).map(src -> src, PropertyDetails::setTransactionType);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getHouseOrientation() != null ? property.getHouseOrientation().name() : null;
                    }).map(src -> src, PropertyDetails::setHouseOrientation);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getBalconyOrientation() != null ? property.getBalconyOrientation().name() : null;
                    }).map(src -> src, PropertyDetails::setBalconyOrientation);

                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        return property.getStatus() != null ? property.getStatus().name() : null;
                    }).map(src -> src, PropertyDetails::setStatus);

                    // Map media list
                    mapper.using(ctx -> {
                        Property property = (Property) ctx.getSource();
                        if (property.getMediaList() != null && !property.getMediaList().isEmpty()) {
                            return property.getMediaList().stream()
                                    .map(media -> MediaResponse.builder()
                                            .id(media.getId())
                                            .mediaType(media.getMediaType() != null ? media.getMediaType().name() : null)
                                            .fileName(media.getFileName())
                                            .filePath(media.getFilePath())
                                            .mimeType(media.getMimeType())
                                            .createdAt(media.getCreatedAt())
                                            .updatedAt(media.getUpdatedAt())
                                            .build())
                                    .collect(Collectors.toList());
                        }
                        return null;
                    }).map(src -> src, PropertyDetails::setMediaList);
                });
    }

    public PropertyDetails toPropertyDetails(PropertyDetailsProjection projection, List<MediaProjection> mediaProjections, List<DocumentProjection> documentProjections) {
        if (projection == null) {
            return null;
        }

        // Map media projections to DTOs
        List<MediaResponse> mediaResponses = mediaProjections != null ? mediaProjections.stream()
                .map(media -> MediaResponse.builder()
                        .id(media.id())
                        .createdAt(media.createdAt())
                        .updatedAt(media.updatedAt())
                        .mediaType(media.mediaType())
                        .fileName(media.fileName())
                        .filePath(media.filePath())
                        .mimeType(media.mimeType())
                        .build())
                .collect(Collectors.toList()) : null;

        // Map document projections to DTOs
        List<DocumentResponse> documentResponses = documentProjections != null ? documentProjections.stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.id())
                        .createdAt(doc.createdAt())
                        .updatedAt(doc.updatedAt())
                        .documentTypeId(doc.documentTypeId())
                        .documentTypeName(doc.documentTypeName())
                        .documentNumber(doc.documentNumber())
                        .documentName(doc.documentName())
                        .filePath(doc.filePath())
                        .issueDate(doc.issueDate())
                        .expiryDate(doc.expiryDate())
                        .issuingAuthority(doc.issuingAuthority())
                        .verificationStatus(doc.verificationStatus())
                        .verifiedAt(doc.verifiedAt())
                        .rejectionReason(doc.rejectionReason())
                        .build())
                .collect(Collectors.toList()) : null;

        // Map projection to DTO
        return PropertyDetails.builder()
                .id(projection.id())
                .createdAt(projection.createdAt())
                .updatedAt(projection.updatedAt())
                .owner(projection.ownerId() != null ? SimpleUserResponse.builder()
                        .id(projection.ownerId())
                        .firstName(projection.ownerFirstName())
                        .lastName(projection.ownerLastName())
                        .phoneNumber(projection.ownerPhoneNumber())
                        .zaloContact(projection.ownerZaloContact())
                        .createdAt(projection.ownerCreatedAt())
                        .updatedAt(projection.ownerUpdatedAt())
                        .build() : null)
                .assignedAgent(projection.agentId() != null ? SimpleUserResponse.builder()
                        .id(projection.agentId())
                        .firstName(projection.agentFirstName())
                        .lastName(projection.agentLastName())
                        .phoneNumber(projection.agentPhoneNumber())
                        .zaloContact(projection.agentZaloContact())
                        .createdAt(projection.agentCreatedAt())
                        .updatedAt(projection.agentUpdatedAt())
                        .build() : null)
                .serviceFeeAmount(projection.serviceFeeAmount())
                .serviceFeeCollectedAmount(projection.serviceFeeCollectedAmount())
                .propertyTypeId(projection.propertyTypeId())
                .propertyTypeName(projection.propertyTypeName())
                .wardId(projection.wardId())
                .wardName(projection.wardName())
                .districtId(projection.districtId())
                .districtName(projection.districtName())
                .cityId(projection.cityId())
                .cityName(projection.cityName())
                .title(projection.title())
                .description(projection.description())
                .transactionType(projection.transactionType())
                .fullAddress(projection.fullAddress())
                .area(projection.area())
                .rooms(projection.rooms())
                .bathrooms(projection.bathrooms())
                .floors(projection.floors())
                .bedrooms(projection.bedrooms())
                .houseOrientation(projection.houseOrientation())
                .balconyOrientation(projection.balconyOrientation())
                .yearBuilt(projection.yearBuilt())
                .priceAmount(projection.priceAmount())
                .pricePerSquareMeter(projection.pricePerSquareMeter())
                .commissionRate(projection.commissionRate())
                .amenities(projection.amenities())
                .status(projection.status())
                .viewCount(projection.viewCount())
                .approvedAt(projection.approvedAt())
                .mediaList(mediaResponses)
                .documentList(documentResponses)
                .build();
    }
}
