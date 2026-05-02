package com.se100.bds.services.domains.customer.impl;

import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.schemas.customer.AbstractCustomerPreferenceMongoSchema;
import com.se100.bds.models.schemas.customer.CustomerFavoriteProperty;
import com.se100.bds.repositories.domains.mongo.customer.*;
import com.se100.bds.services.domains.customer.CustomerFavoriteService;
import com.se100.bds.services.domains.customer.CustomerPreferenceEntityFactory;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.dtos.results.PropertyCard;
import com.se100.bds.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerFavoriteServiceImpl implements CustomerFavoriteService {
    private final CustomerFavoritePropertyRepository customerFavoritePropertyRepository;
    private final CustomerPreferredCityRepository customerPreferredCityRepository;
    private final CustomerPreferredDistrictRepository customerPreferredDistrictRepository;
    private final CustomerPreferredPropertyTypeRepository customerPreferredPropertyTypeRepository;
    private final CustomerPreferredWardRepository customerPreferredWardRepository;
    private final UserService userService;
    private final PropertyService propertyService;

    public CustomerFavoriteServiceImpl(
            CustomerFavoritePropertyRepository customerFavoritePropertyRepository,
            CustomerPreferredCityRepository customerPreferredCityRepository,
            CustomerPreferredDistrictRepository customerPreferredDistrictRepository,
            CustomerPreferredPropertyTypeRepository customerPreferredPropertyTypeRepository,
            CustomerPreferredWardRepository customerPreferredWardRepository,
            UserService userService,
            @Lazy PropertyService propertyService) {
        this.customerFavoritePropertyRepository = customerFavoritePropertyRepository;
        this.customerPreferredCityRepository = customerPreferredCityRepository;
        this.customerPreferredDistrictRepository = customerPreferredDistrictRepository;
        this.customerPreferredPropertyTypeRepository = customerPreferredPropertyTypeRepository;
        this.customerPreferredWardRepository = customerPreferredWardRepository;
        this.userService = userService;
        this.propertyService = propertyService;
    }

    private Map<Constants.LikeTypeEnum, BaseCustomerPreferenceRepository<? extends AbstractCustomerPreferenceMongoSchema>> repositoryMap;

    private void initRepositoryMap() {
        if (repositoryMap == null) {
            repositoryMap = Map.of(
                    Constants.LikeTypeEnum.PROPERTY, customerFavoritePropertyRepository,
                    Constants.LikeTypeEnum.CITY, customerPreferredCityRepository,
                    Constants.LikeTypeEnum.DISTRICT, customerPreferredDistrictRepository,
                    Constants.LikeTypeEnum.WARD, customerPreferredWardRepository,
                    Constants.LikeTypeEnum.PROPERTY_TYPE, customerPreferredPropertyTypeRepository
            );
        }
    }

    private BaseCustomerPreferenceRepository<? extends AbstractCustomerPreferenceMongoSchema> getRepository(Constants.LikeTypeEnum likeType) {
        initRepositoryMap();
        return repositoryMap.get(likeType);
    }

    @Override
    public boolean like(UUID id, Constants.LikeTypeEnum likeType) {
        UUID customerId = userService.getUserId();
        BaseCustomerPreferenceRepository<? extends AbstractCustomerPreferenceMongoSchema> repository = getRepository(likeType);
        if (repository == null) {
            log.error("Invalid like type: {}", likeType);
            return false;
        }

        try {
            if (isLike(id, customerId, likeType)) {
                repository.deleteByCustomerIdAndRefId(customerId, id);
                log.info("Removed {} preference {} for customer {}", likeType, id, customerId);
                return false;
            } else {
                AbstractCustomerPreferenceMongoSchema entity = CustomerPreferenceEntityFactory.createEntity(
                        likeType, customerId, id);
                saveEntity(repository, entity);
                log.info("Added {} preference {} for customer {}", likeType, id, customerId);
                return true;
            }
        } catch (Exception e) {
            log.error("Error processing like operation for type {} and id {}: {}", likeType, id, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isLike(UUID refId, UUID customerId, Constants.LikeTypeEnum likeType) {
        BaseCustomerPreferenceRepository<? extends AbstractCustomerPreferenceMongoSchema> repository = getRepository(likeType);
        if (repository == null) {
            log.error("Invalid like type: {}", likeType);
            return false;
        }

        try {
            return repository.existsByCustomerIdAndRefId(customerId, refId);
        } catch (Exception e) {
            log.error("Error checking like status for type {} and id {}: {}", likeType, refId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isLikeByMe(UUID refId, Constants.LikeTypeEnum likeType) {
        try {
            User currentUser = userService.getUser();
            return isLike(refId, currentUser.getId(), likeType);
        } catch (Exception e) {
            log.error("Error checking like status for type {} and id {}", likeType, e.getMessage());
            return false;
        }
    }

    @Override
    public Page<SimplePropertyCard> getFavoritePropertyCards(Pageable pageable) {
        UUID customerId = userService.getUserId();

        // Get all favorite property IDs from MongoDB
        List<CustomerFavoriteProperty> favorites = customerFavoritePropertyRepository.findByCustomerId(customerId);
        List<UUID> propertyIds = favorites.stream()
                .map(CustomerFavoriteProperty::getRefId)
                .collect(Collectors.toList());

        if (propertyIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Get property cards from PropertyService
        Page<PropertyCard> propertyCards = propertyService.getFavoritePropertyCards(propertyIds, pageable);

        // Map PropertyCard to SimplePropertyCard
        List<SimplePropertyCard> simpleCards = propertyCards.getContent().stream()
                .map(card -> SimplePropertyCard.builder()
                        .id(card.getId())
                        .title(card.getTitle())
                        .thumbnailUrl(card.getThumbnailUrl())
                        .transactionType(card.getTransactionType() != null ? Constants.TransactionTypeEnum.get(card.getTransactionType()) : null)
                        .isFavorite(true)
                        .numberOfImages(card.getNumberOfImages())
                        .location(card.getDistrict() + ", " + card.getCity())
                        .status(card.getStatus())
                        .price(card.getPrice())
                        .totalArea(card.getTotalArea())
                        .ownerId(card.getOwnerId())
                        .ownerFirstName(card.getOwnerFirstName())
                        .ownerLastName(card.getOwnerLastName())
                        .ownerTier(card.getOwnerTier())
                        .agentId(card.getAgentId())
                        .agentFirstName(card.getAgentFirstName())
                        .agentLastName(card.getAgentLastName())
                        .agentTier(card.getAgentTier())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(simpleCards, pageable, propertyCards.getTotalElements());
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractCustomerPreferenceMongoSchema> void saveEntity(
            BaseCustomerPreferenceRepository<? extends AbstractCustomerPreferenceMongoSchema> repository,
            AbstractCustomerPreferenceMongoSchema entity) {
        ((BaseCustomerPreferenceRepository<T>) repository).save((T) entity);
    }
}
