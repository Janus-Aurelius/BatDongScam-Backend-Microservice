package com.se100.bds.services.domains.property;

import com.se100.bds.dtos.requests.property.CreatePropertyTypeRequest;
import com.se100.bds.dtos.requests.property.UpdatePropertyTypeRequest;
import com.se100.bds.dtos.requests.property.CreatePropertyRequest;
import com.se100.bds.dtos.requests.property.UpdatePropertyRequest;
import com.se100.bds.dtos.requests.property.UpdatePropertyStatusRequest;
import com.se100.bds.dtos.responses.property.PropertyContractHistoryDatapoint;
import com.se100.bds.dtos.responses.property.PropertyDetails;
import com.se100.bds.dtos.responses.property.PropertyTypeResponse;
import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.services.dtos.results.PropertyCard;
import com.se100.bds.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PropertyService {
    Page<Property> getAll(Pageable pageable);

    Property findPropertyById(UUID propertyId);

    Page<PropertyCard> getAllCardsWithFilters(List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds,
                                              List<UUID> propertyTypeIds, UUID ownerId, String ownerName,
                                              List<Constants.ContributionTierEnum> ownerTier,
                                              UUID agentId, String agentName,
                                              List<Constants.PerformanceTierEnum> agentTier, Boolean hasAgent,
                                              BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minArea, BigDecimal maxArea,
                                              Integer rooms, Integer bathrooms, Integer bedrooms, Integer floors, Constants.OrientationEnum houseOrientation, Constants.OrientationEnum balconyOrientation,
                                              List<Constants.TransactionTypeEnum> transactionType, List<Constants.PropertyStatusEnum> statuses, boolean topK,
                                              Pageable pageable);
    Page<PropertyType> getAllTypes(Pageable pageable);
    PropertyDetails getPropertyDetailsById(UUID propertyId);
    List<Property> getAllByUserIdAndStatus(UUID ownerId, UUID customerId, UUID salesAgentId, List<Constants.PropertyStatusEnum> statuses);
    PropertyDetails createProperty(CreatePropertyRequest request, MultipartFile[] mediaFiles, MultipartFile[] documents);
    PropertyDetails updateProperty(UUID propertyId, UpdatePropertyRequest request, MultipartFile[] mediaFiles, MultipartFile[] documents);
    PropertyDetails updatePropertyStatus(UUID propertyId, UpdatePropertyStatusRequest request);
    void deleteProperty(UUID propertyId);
    void assignAgentToProperty(UUID agentId, UUID propertyId);

    // PropertyType CRUD operations
    PropertyTypeResponse createPropertyType(CreatePropertyTypeRequest request) throws IOException;
    PropertyTypeResponse updatePropertyType(UpdatePropertyTypeRequest request) throws IOException;
    void deletePropertyType(UUID id) throws IOException;

    // Helper methods
    int countByAssignedAgentId(UUID agentId);

    // Assignment
    boolean assignAgent(UUID agentId, UUID propertyId);
    Page<SimplePropertyCard> myAssignedProperties(
            Pageable pageable,
            String propertyOwnerName
    );

    List<UUID> getAllAvailablePropertyTypeIds();

    String getPropertyTypeName(UUID propertyTypeId);

    int countPropertiesByPropertyTypeId(UUID propertyTypeId);

    Page<PropertyCard> getFavoritePropertyCards(List<UUID> propertyIds, Pageable pageable);

    List<PropertyContractHistoryDatapoint> getPropertyContractHistory(UUID propertyId, boolean includePastContracts);
}
