package com.se100.bds.services.domains.property.impl;

import com.se100.bds.dtos.responses.property.PropertyContractHistoryDatapoint;
import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.dtos.requests.property.CreatePropertyRequest;
import com.se100.bds.dtos.requests.property.CreatePropertyTypeRequest;
import com.se100.bds.dtos.requests.property.DocumentUploadInfo;
import com.se100.bds.dtos.requests.property.MediaUploadInfo;
import com.se100.bds.dtos.requests.property.UpdatePropertyRequest;
import com.se100.bds.dtos.requests.property.UpdatePropertyStatusRequest;
import com.se100.bds.dtos.requests.property.UpdatePropertyTypeRequest;
import com.se100.bds.dtos.responses.property.PropertyDetails;
import com.se100.bds.dtos.responses.property.PropertyTypeResponse;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.mappers.PropertyMapper;
import com.se100.bds.models.entities.document.DocumentType;
import com.se100.bds.models.entities.document.IdentificationDocument;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.contract.ContractRepository;
import com.se100.bds.repositories.domains.document.DocumentTypeRepository;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.property.PropertyTypeRepository;
import com.se100.bds.repositories.domains.user.PropertyOwnerRepository;
import com.se100.bds.repositories.dtos.DocumentProjection;
import com.se100.bds.repositories.dtos.MediaProjection;
import com.se100.bds.repositories.dtos.PropertyCardProtection;
import com.se100.bds.repositories.dtos.PropertyDetailsProjection;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.domains.payment.PaymentService;
import com.se100.bds.services.domains.customer.CustomerFavoriteService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.ranking.RankingService;
import com.se100.bds.services.domains.search.SearchService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.dtos.results.PropertyCard;
import com.se100.bds.services.fileupload.CloudinaryService;
import com.se100.bds.utils.Constants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private static final EnumSet<Constants.PropertyStatusEnum> ADMIN_STATUS_UPDATABLE = EnumSet.of(
            Constants.PropertyStatusEnum.PENDING,
            Constants.PropertyStatusEnum.APPROVED,
            Constants.PropertyStatusEnum.REJECTED,
            Constants.PropertyStatusEnum.AVAILABLE,
            Constants.PropertyStatusEnum.DELETED
    );

    private static final EnumSet<Constants.PropertyStatusEnum> OWNER_STATUS_UPDATABLE = EnumSet.of(
            Constants.PropertyStatusEnum.RENTED,
            Constants.PropertyStatusEnum.SOLD,
            Constants.PropertyStatusEnum.AVAILABLE,
            Constants.PropertyStatusEnum.UNAVAILABLE
    );

    private final PropertyRepository propertyRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final WardRepository wardRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PropertyMapper propertyMapper;
    private final UserService userService;
    private final SearchService searchService;
    private final RankingService rankingService;
    private final CloudinaryService cloudinaryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final CustomerFavoriteService customerFavoriteService;
    private final ContractRepository contractRepository;

    @Override
    public Page<Property> getAll(Pageable pageable) {
        return propertyRepository.findAll(pageable);
    }

    @Override
    public Property findPropertyById(UUID propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));
    }

    @Override
    public Page<PropertyCard> getAllCardsWithFilters(List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds,
                                                     List<UUID> propertyTypeIds, UUID ownerId, String ownerName,
                                                     List<Constants.ContributionTierEnum> ownerTier,
                                                     UUID agentId, String agentName, List<Constants.PerformanceTierEnum> agentTier, Boolean hasAgent,
                                                     BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minArea, BigDecimal maxArea,
                                                     Integer rooms, Integer bathrooms, Integer bedrooms, Integer floors,
                                                     Constants.OrientationEnum houseOrientation, Constants.OrientationEnum balconyOrientation,
                                                     List<Constants.TransactionTypeEnum> transactionType,
                                                     List<Constants.PropertyStatusEnum> statuses, boolean topK,
                                                     Pageable pageable) {

        User currentUser = null;
        try {
            currentUser = userService.getUser();
            searchService.addSearchList(currentUser.getId(), cityIds, districtIds, wardIds, propertyTypeIds);
        } catch (Exception ignored) {
        }

        List<UUID> propertyIds = null;
        if (topK) {
            // Lấy tháng và năm hiện tại
            int currentYear = LocalDateTime.now().getYear();
            int currentMonth = LocalDateTime.now().getMonthValue();

            propertyIds = searchService.getMostSearchedPropertyIds(1000, currentYear, currentMonth);
            log.info("Found {} most searched properties", propertyIds.size());
        }

        List<String> transactionTypeStrings = (transactionType != null && !transactionType.isEmpty())
                ? transactionType.stream().map(Enum::name).toList()
                : null;

        List<UUID> ownerIds;
        if (ownerId != null) {
            ownerIds = List.of(ownerId);
        } else {
            String searchName = ownerName != null ? ownerName : "";
            List<User> owners = userService.getAllByName(searchName);

            if (ownerTier != null && !ownerTier.isEmpty()) {
                int currentMonth = LocalDateTime.now().getMonthValue();
                int currentYear = LocalDateTime.now().getYear();

                ownerIds = owners.stream()
                        .map(User::getId)
                        .filter(id -> {
                            String tier = rankingService.getTier(id, Constants.RoleEnum.PROPERTY_OWNER, currentMonth, currentYear);
                            return tier != null && ownerTier.stream().anyMatch(filter -> filter.name().equals(tier));
                        })
                        .toList();
            } else {
                ownerIds = owners.stream()
                        .map(User::getId)
                        .toList();
            }
        }

        List<UUID> agentIds;
        if (hasAgent) {
            if (agentId != null) {
                agentIds = List.of(agentId);
            } else {
                String searchName = agentName != null ? agentName : "";
                List<User> agents = userService.getAllByName(searchName);
                if (agentTier != null && !agentTier.isEmpty()) {
                    int currentMonth = LocalDateTime.now().getMonthValue();
                    int currentYear = LocalDateTime.now().getYear();

                    agentIds = agents.stream()
                            .map(User::getId)
                            .filter(id -> {
                                String tier = rankingService.getTier(id, Constants.RoleEnum.SALESAGENT, currentMonth, currentYear);
                                return tier != null && agentTier.stream().anyMatch(filter -> filter.name().equals(tier));
                            })
                            .toList();
                } else {
                    agentIds = agents.stream()
                            .map(User::getId)
                            .toList();
                }
            }
        } else {
            agentIds = null;
        }

        List<String> statusStrings = (statuses != null && !statuses.isEmpty())
                ? statuses.stream().map(Enum::name).toList()
                : null;

        String houseOrientationValue = houseOrientation != null ? houseOrientation.getValue() : null;
        String balconyOrientationValue = balconyOrientation != null ? balconyOrientation.getValue() : null;

        Page<PropertyCardProtection> cardProtections = propertyRepository.findAllPropertyCardsWithFilter(
                pageable,
                propertyIds,
                cityIds,
                districtIds,
                wardIds,
                propertyTypeIds,
                ownerIds,
                agentIds,
                minPrice,
                maxPrice,
                minArea,
                maxArea,
                rooms,
                bathrooms,
                bedrooms,
                floors,
                houseOrientationValue,
                balconyOrientationValue,
                transactionTypeStrings,
                statusStrings,
                currentUser != null ? currentUser.getId() : null
        );

        // Create a map of ID to transactionType for quick lookup
        Map<UUID, String> transactionTypeMap = new HashMap<>();
        for (PropertyCardProtection protection : cardProtections) {
            if (protection.transactionType() != null) {
                transactionTypeMap.put(protection.id(), protection.transactionType().name());
            }
        }

        Page<PropertyCard> propertyCardsPage = propertyMapper.mapToPage(cardProtections, PropertyCard.class);

        for (PropertyCard propertyCard : propertyCardsPage) {
            // Set transactionType from the protection record
            propertyCard.setTransactionType(transactionTypeMap.get(propertyCard.getId()));

            if (propertyCard.getOwnerId() != null) {
                propertyCard.setOwnerTier(rankingService.getCurrentTier(
                        propertyCard.getOwnerId(),
                        Constants.RoleEnum.PROPERTY_OWNER
                ));
            }
            if (propertyCard.getAgentId() != null) {
                propertyCard.setAgentTier(rankingService.getCurrentTier(
                        propertyCard.getAgentId(),
                        Constants.RoleEnum.SALESAGENT
                ));
            }
            // mark favorite if current user liked this property
            if (currentUser != null && customerFavoriteService.isLike(
                    propertyCard.getId(), currentUser.getId(), Constants.LikeTypeEnum.PROPERTY)) {
                propertyCard.setFavorite(true);
            }
        }

        return propertyCardsPage;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyType> getAllTypes(Pageable pageable) {
        return propertyTypeRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyDetails getPropertyDetailsById(UUID propertyId) {
        PropertyDetailsProjection projection = propertyRepository.findPropertyDetailsById(propertyId);
        if (projection == null) {
            throw new RuntimeException("Property not found with id: " + propertyId);
        }

        // Get media list
        List<MediaProjection> mediaProjections = propertyRepository.findMediaByPropertyId(propertyId);

        // Get document list
        List<DocumentProjection> documentProjections = propertyRepository.findDocumentsByPropertyId(propertyId);

        // Use mapper to convert projection to DTO
        PropertyDetails propertyDetails = propertyMapper.toPropertyDetails(projection, mediaProjections, documentProjections);

        // Set tier for owner
        if (propertyDetails.getOwner() != null && propertyDetails.getOwner().getId() != null) {
            String ownerTier = rankingService.getCurrentTier(
                    propertyDetails.getOwner().getId(),
                    Constants.RoleEnum.PROPERTY_OWNER
            );
            propertyDetails.getOwner().setTier(ownerTier);
        }

        // Set tier for agent
        if (propertyDetails.getAssignedAgent() != null && propertyDetails.getAssignedAgent().getId() != null) {
            String agentTier = rankingService.getCurrentTier(
                    propertyDetails.getAssignedAgent().getId(),
                    Constants.RoleEnum.SALESAGENT
            );
            propertyDetails.getAssignedAgent().setTier(agentTier);
        }

        return propertyDetails;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Property> getAllByUserIdAndStatus(UUID ownerId, UUID customerId, UUID salesAgentId, List<Constants.PropertyStatusEnum> statuses) {
        if (customerId != null) {
            return propertyRepository.findAllByCustomer_IdAndStatusIn(customerId);
        }
        if (salesAgentId == null) {
            if (statuses == null || statuses.isEmpty())
                return propertyRepository.findAllByOwner_Id(ownerId);
            return propertyRepository.findAllByOwner_IdAndStatusIn(ownerId, statuses);
        }
        if (ownerId == null) {
            if (statuses == null || statuses.isEmpty())
                return propertyRepository.findAllByAssignedAgent_Id(salesAgentId);
            return propertyRepository.findAllByAssignedAgent_IdAndStatusIn(salesAgentId, statuses);
        }
        if (statuses == null || statuses.isEmpty())
            return propertyRepository.findAllByOwner_IdAndAssignedAgent_Id(ownerId, salesAgentId);
        return propertyRepository.findAllByOwner_IdAndAssignedAgent_IdAndStatusIn(ownerId, salesAgentId, statuses);
    }

    @Override
    @Transactional
    public PropertyDetails createProperty(CreatePropertyRequest request, MultipartFile[] mediaFiles, MultipartFile[] documents) {
        User currentUser = userService.getUser();
        boolean isAdmin = isAdmin(currentUser);
        boolean isOwner = isPropertyOwner(currentUser);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to create properties");
        }

        // Validate compulsory documents are provided
        validateCompulsoryDocuments(request.getDocumentMetadata(), isAdmin);

        PropertyOwner owner = resolveOwnerForCreate(isAdmin, request.getOwnerId(), currentUser.getId());
        PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId())
                .orElseThrow(() -> new NotFoundException("Property type not found with id: " + request.getPropertyTypeId()));
        Ward ward = wardRepository.findById(request.getWardId())
                .orElseThrow(() -> new NotFoundException("Ward not found with id: " + request.getWardId()));

        BigDecimal commissionRate = Constants.DEFAULT_PROPERTY_COMMISSION_RATE;
        BigDecimal serviceFeeAmount = computeServiceFee(request.getPriceAmount(), commissionRate);

        var property = new Property();

        applyPropertyChanges(property, request, owner, propertyType, ward, commissionRate, serviceFeeAmount);
        // if admin, mark service fee as collected and set status to AVAILABLE
        if (isAdmin) {
            property.setStatus(Constants.PropertyStatusEnum.AVAILABLE);
            property.setServiceFeeCollectedAmount(serviceFeeAmount);
            property.setApprovedAt(LocalDateTime.now());
        } else {
            property.setStatus(Constants.PropertyStatusEnum.PENDING);
        }

        ensureMediaCollection(property);
        ensureDocumentCollection(property);
        Property persisted = propertyRepository.save(property);

        addMediaFiles(persisted, mediaFiles, request.getMediaMetadata());
        addDocumentFiles(persisted, documents, request.getDocumentMetadata());

        Property saved = propertyRepository.save(persisted);
        if (isAdmin) {
            log.info("Admin created property {} for owner {}", saved.getId(), owner.getId());
        } else {
            log.info("Owner {} created property {}", owner.getId(), saved.getId());
            // Track property listing action for ranking
            if (saved.getTransactionType() == Constants.TransactionTypeEnum.SALE) {
                rankingService.propertyOwnerAction(owner.getId(), Constants.PropertyOwnerActionEnum.PROPERTY_FOR_SALE_LISTED, null);
            } else if (saved.getTransactionType() == Constants.TransactionTypeEnum.RENTAL) {
                rankingService.propertyOwnerAction(owner.getId(), Constants.PropertyOwnerActionEnum.PROPERTY_FOR_RENT_LISTED, null);
            }
        }
        return propertyMapper.mapTo(saved, PropertyDetails.class);
    }

    @Override
    @Transactional
    public PropertyDetails updateProperty(UUID propertyId, UpdatePropertyRequest request, MultipartFile[] mediaFiles, MultipartFile[] documents) {
        User currentUser = userService.getUser();
        boolean isAdmin = isAdmin(currentUser);
        boolean isOwner = isPropertyOwner(currentUser);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found with id: " + propertyId));

        if (!isAdmin && (!isOwner || !property.getOwner().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("You do not have permission to modify this property");
        }

        if (property.getStatus() == Constants.PropertyStatusEnum.DELETED) {
            throw new IllegalStateException("Cannot update a deleted property");
        }

        PropertyOwner owner = resolveOwnerForUpdate(isAdmin, request.getOwnerId(), property.getOwner());
        PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId())
                .orElseThrow(() -> new NotFoundException("Property type not found with id: " + request.getPropertyTypeId()));
        Ward ward = wardRepository.findById(request.getWardId())
                .orElseThrow(() -> new NotFoundException("Ward not found with id: " + request.getWardId()));

        BigDecimal commissionRate = Constants.DEFAULT_PROPERTY_COMMISSION_RATE;
        BigDecimal serviceFeeAmount = computeServiceFee(request.getPriceAmount(), commissionRate);

        applyPropertyChanges(property, request, owner, propertyType, ward, commissionRate, serviceFeeAmount);

        if (isAdmin) {
            property.setServiceFeeCollectedAmount(property.getServiceFeeAmount());
            property.setStatus(Constants.PropertyStatusEnum.AVAILABLE);
            if (property.getApprovedAt() == null) {
                property.setApprovedAt(LocalDateTime.now());
            }
        } else {
            // always set back to pending if owner updates
            property.setStatus(Constants.PropertyStatusEnum.PENDING);
        }

        //! WARN: this code is trash. this 100% will cause a bug related to media management or when admin refuses to approve
        ensureMediaCollection(property);
        ensureDocumentCollection(property);
        removeMediaFiles(property, request.getMediaIdsToRemove());
        removeDocumentFiles(property, request.getDocumentIdsToRemove());
        addMediaFiles(property, mediaFiles, request.getMediaMetadata());
        addDocumentFiles(property, documents, request.getDocumentMetadata());

        // Validate that property still has all compulsory documents after update
        validatePropertyHasCompulsoryDocuments(property, isAdmin);

        Property saved = propertyRepository.save(property);
        return propertyMapper.mapTo(saved, PropertyDetails.class);
    }

    @Override
    @Transactional
    public PropertyDetails updatePropertyStatus(UUID propertyId, UpdatePropertyStatusRequest request) {
        User currentUser = userService.getUser();
        boolean isAdmin = isAdmin(currentUser);
        boolean isOwner = isPropertyOwner(currentUser);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found with id: " + propertyId));

        if (property.getStatus() == Constants.PropertyStatusEnum.DELETED) {
            throw new IllegalStateException("Cannot update a deleted property");
        }

        Constants.PropertyStatusEnum targetStatus = request.getStatus();

        if (isAdmin) {
            return handleAdminUpdateStatus(property, targetStatus);
        }

        if (isOwner && property.getOwner().getId().equals(currentUser.getId())) {
            return handleOwnerUpdateStatus(targetStatus, property, currentUser);
        }

        throw new AccessDeniedException("You do not have permission to modify this property");
    }

    private PropertyDetails handleOwnerUpdateStatus(Constants.PropertyStatusEnum targetStatus, Property property, User currentUser) {
        if (!OWNER_STATUS_UPDATABLE.contains(targetStatus)) {
            throw new IllegalArgumentException("Property owners can only set status to RENTED, SOLD, AVAILABLE or UNAVAILABLE");
        }

        property.setStatus(targetStatus);
        Property saved = propertyRepository.save(property);
        log.info("Owner {} updated property {} status to {}", currentUser.getId(), saved.getId(), targetStatus);

        // Track property status change action for ranking
        if (targetStatus == Constants.PropertyStatusEnum.SOLD) {
            rankingService.propertyOwnerAction(property.getOwner().getId(), Constants.PropertyOwnerActionEnum.PROPERTY_SOLD, null);
        } else if (targetStatus == Constants.PropertyStatusEnum.RENTED) {
            rankingService.propertyOwnerAction(property.getOwner().getId(), Constants.PropertyOwnerActionEnum.PROPERTY_RENTED, null);
        }

        return propertyMapper.mapTo(saved, PropertyDetails.class);
    }

    private PropertyDetails handleAdminUpdateStatus(Property property, Constants.PropertyStatusEnum targetStatus) {
        if (!ADMIN_STATUS_UPDATABLE.contains(targetStatus)) {
            throw new IllegalArgumentException("Unsupported status update: " + targetStatus);
        }

        if (targetStatus == Constants.PropertyStatusEnum.AVAILABLE && hasOutstandingServiceFee(property)) {
            throw new IllegalStateException("Cannot mark property as AVAILABLE while service fee payment is outstanding");
        }

        switch (targetStatus) {
            case APPROVED -> {
                property.setApprovedAt(LocalDateTime.now());
                if (hasOutstandingServiceFee(property))
                    paymentService.createServiceFeePayment(property);
                // notify owner about approval
                notificationService.createNotification(
                        property.getOwner().getUser(),
                        Constants.NotificationTypeEnum.PROPERTY_APPROVAL,
                        "Property Approved",
                        "Your property \"" + property.getTitle() + "\" has been approved and is now listed.",
                        Constants.RelatedEntityTypeEnum.PROPERTY,
                        property.getId().toString(),
                        null
                );
            }
            case PENDING -> {
                property.setApprovedAt(null);
                property.setAssignedAgent(null);
            }
            case REJECTED -> {
                property.setApprovedAt(null);
                // TODO: verify that we need to do this
                property.setAssignedAgent(null);
                // notify owner about rejection
                notificationService.createNotification(
                        property.getOwner().getUser(),
                        Constants.NotificationTypeEnum.PROPERTY_REJECTION,
                        "Property Rejected",
                        "Your property \"" + property.getTitle() + "\" has been rejected, please review.",
                        Constants.RelatedEntityTypeEnum.PROPERTY,
                        property.getId().toString(),
                        null
                );
            }
            case AVAILABLE -> {
                if (property.getApprovedAt() == null) {
                    property.setApprovedAt(LocalDateTime.now());
                }
            }
            default -> log.warn("Ignoring unhandled actions for admin status update to {}", targetStatus);
        }

        property.setStatus(targetStatus);
        Property saved = propertyRepository.save(property);
        log.info("Admin updated property {} status to {}", saved.getId(), targetStatus);
        return propertyMapper.mapTo(saved, PropertyDetails.class);
    }

    @Override
    @Transactional
    public void deleteProperty(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found with id: " + propertyId));

        if (property.getStatus() == Constants.PropertyStatusEnum.DELETED) {
            return;
        }

        // Check ownership if not admin
        User currentUser = userService.getUser();
        boolean isAdmin = currentUser.getRole() == Constants.RoleEnum.ADMIN;

        if (!isAdmin) {
            // Property owner must own this property
            if (property.getOwner() == null ||
                    !property.getOwner().getUser().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only delete your own properties");
            }
        }

        property.setStatus(Constants.PropertyStatusEnum.DELETED);
        property.setApprovedAt(null);
        property.setAssignedAgent(null);
        propertyRepository.save(property);
        log.info("Soft deleted property {}", propertyId);
    }

    @Override
    @Transactional
    public void assignAgentToProperty(UUID agentId, UUID propertyId) {
        SaleAgent salesAgent = userService.findSaleAgentById(agentId);
        Property assignedProperty = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found with id: " + propertyId));
        assignedProperty.setAssignedAgent(salesAgent);
    }

    @Override
    @Transactional
    public PropertyTypeResponse createPropertyType(CreatePropertyTypeRequest request) throws IOException {
        // Upload avatar if provided
        String avatarUrl = null;
        if (request.getAvatar() != null) {
            avatarUrl = cloudinaryService.uploadFile(request.getAvatar(), "property-types");
        }

        // Create new PropertyType
        PropertyType propertyType = PropertyType.builder()
                .typeName(request.getTypeName())
                .avatarUrl(avatarUrl)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        PropertyType savedPropertyType = propertyTypeRepository.save(propertyType);
        log.info("Created new property type with id: {}", savedPropertyType.getId());

        return propertyMapper.mapTo(savedPropertyType, PropertyTypeResponse.class);
    }

    @Override
    @Transactional
    public PropertyTypeResponse updatePropertyType(UpdatePropertyTypeRequest request) throws IOException {
        PropertyType propertyType = propertyTypeRepository.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Property type not found with id: " + request.getId()));

        // Upload new avatar if provided
        String newAvatarUrl = null;
        if (request.getAvatar() != null) {
            newAvatarUrl = cloudinaryService.uploadFile(request.getAvatar(), "property-types");

            // Delete old avatar if exists
            if (propertyType.getAvatarUrl() != null) {
                try {
                    cloudinaryService.deleteFile(propertyType.getAvatarUrl());
                } catch (IOException e) {
                    log.error("Failed to delete old avatar from Cloudinary: {}", e.getMessage());
                }
            }
        }

        // Update fields only if they are not null
        if (request.getTypeName() != null) {
            propertyType.setTypeName(request.getTypeName());
        }
        if (newAvatarUrl != null) {
            propertyType.setAvatarUrl(newAvatarUrl);
        }
        if (request.getDescription() != null) {
            propertyType.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            propertyType.setIsActive(request.getIsActive());
        }

        PropertyType updatedPropertyType = propertyTypeRepository.save(propertyType);
        log.info("Updated property type with id: {}", updatedPropertyType.getId());

        return propertyMapper.mapTo(updatedPropertyType, PropertyTypeResponse.class);
    }

    @Override
    @Transactional
    public void deletePropertyType(UUID id) throws IOException {
        PropertyType propertyType = propertyTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property type not found with id: " + id));

        // Delete avatar from Cloudinary if exists
        if (propertyType.getAvatarUrl() != null) {
            try {
                cloudinaryService.deleteFile(propertyType.getAvatarUrl());
            } catch (IOException e) {
                log.error("Failed to delete avatar from Cloudinary: {}", e.getMessage());
                throw e; // rethrow to fail request
            }
        }

        propertyTypeRepository.delete(propertyType);
        log.info("Deleted property type with id: {}", id);
    }

    @Override
    public int countByAssignedAgentId(UUID agentId) {
        Long count = propertyRepository.countByAssignedAgent_Id(agentId);
        return count != null ? count.intValue() : 0;
    }

    @Override
    public boolean assignAgent(UUID agentId, UUID propertyId) {
        // Find the property
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));

        // If agentId is null, remove current agent
        if (agentId == null) {
            if (property.getAssignedAgent() != null) {
                property.setAssignedAgent(null);
                propertyRepository.save(property);
                log.info("Removed agent from property: {}", propertyId);
                return true;
            }
            return false; // No agent was assigned
        }

        // Find the new agent
        User agentUser = userService.findById(agentId);
        if (agentUser == null || agentUser.getSaleAgent() == null) {
            throw new IllegalArgumentException("User is not a sales agent");
        }

        // Remove old agent if exists and assign new agent
        if (property.getAssignedAgent() != null) {
            log.info("Replacing agent {} with {} for property: {}",
                    property.getAssignedAgent().getId(), agentId, propertyId);
        }

        property.setAssignedAgent(agentUser.getSaleAgent());
        propertyRepository.save(property);
        log.info("Assigned agent {} to property: {}", agentId, propertyId);

        // Track property assignment action for agent ranking
        rankingService.agentAction(agentId, Constants.AgentActionEnum.PROPERTY_ASSIGNED, null);

        return true;
    }

    @Override
    public Page<SimplePropertyCard> myAssignedProperties(
            Pageable pageable,
            String propertyOwnerName) {
        Page<Property> properties;
        if (propertyOwnerName != null && !propertyOwnerName.isEmpty()) {
            List<User> propOwners = userService.findAllByNameAndRole(
                    propertyOwnerName, Constants.RoleEnum.PROPERTY_OWNER
            );
            List<UUID> propOwnerIds = new ArrayList<>();
            if (propOwners != null && !propOwners.isEmpty())
                propOwnerIds = propOwners.stream().map(User::getId).toList();
            properties = propertyRepository.findAllByOwner_IdInAndAssignedAgent_Id(
                    propOwnerIds, userService.getUserId(), pageable
            );
        } else
            properties = propertyRepository.findAllByAssignedAgent_Id(
                    userService.getUserId(), pageable
            );

        Page<SimplePropertyCard> propertyCards = propertyMapper.mapToPage(properties, SimplePropertyCard.class);

        // Enrich with owner and agent tiers
        propertyCards.forEach(card -> {
            if (card.getOwnerId() != null) {
                card.setOwnerTier(rankingService.getCurrentTier(
                        card.getOwnerId(),
                        Constants.RoleEnum.PROPERTY_OWNER
                ));
            }
            if (card.getAgentId() != null) {
                card.setAgentTier(rankingService.getCurrentTier(
                        card.getAgentId(),
                        Constants.RoleEnum.SALESAGENT
                ));
            }
        });

        return propertyCards;
    }

    @Override
    public List<UUID> getAllAvailablePropertyTypeIds() {
        return propertyTypeRepository.getAllIds();
    }

    @Override
    public String getPropertyTypeName(UUID propertyTypeId) {
        return propertyTypeRepository.getPropertyTypeNameById(propertyTypeId);
    }

    @Override
    public int countPropertiesByPropertyTypeId(UUID propertyTypeId) {
        return propertyRepository.countByPropertyType_Id(propertyTypeId);
    }

    private void synchronizeServiceFeeCollection(Property property) {
        BigDecimal serviceFeeAmount = property.getServiceFeeAmount();
        BigDecimal collected = property.getServiceFeeCollectedAmount();

        if (serviceFeeAmount == null) {
            property.setServiceFeeCollectedAmount(BigDecimal.ZERO);
            return;
        }

        if (collected == null || collected.compareTo(BigDecimal.ZERO) < 0) {
            property.setServiceFeeCollectedAmount(BigDecimal.ZERO);
            return;
        }

        if (collected.compareTo(serviceFeeAmount) > 0) {
            property.setServiceFeeCollectedAmount(serviceFeeAmount);
        }
    }

    private boolean hasOutstandingServiceFee(Property property) {
        BigDecimal serviceFeeAmount = property.getServiceFeeAmount();
        if (serviceFeeAmount == null || serviceFeeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal collected = property.getServiceFeeCollectedAmount();

        return serviceFeeAmount.compareTo(collected) > 0;
    }

    private void applyPropertyChanges(Property property,
                                      CreatePropertyRequest request,
                                      PropertyOwner owner,
                                      PropertyType propertyType,
                                      Ward ward,
                                      BigDecimal commissionRate,
                                      BigDecimal serviceFeeAmount) {
        var pricePerSquareMeter = request.getPriceAmount().divide(request.getArea(), 2, RoundingMode.HALF_UP);

        property.setOwner(owner);
        property.setPropertyType(propertyType);
        property.setWard(ward);
        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setTransactionType(request.getTransactionType());
        property.setFullAddress(request.getFullAddress());
        property.setArea(request.getArea());
        property.setRooms(request.getRooms());
        property.setBathrooms(request.getBathrooms());
        property.setFloors(request.getFloors());
        property.setBedrooms(request.getBedrooms());
        property.setHouseOrientation(request.getHouseOrientation());
        property.setBalconyOrientation(request.getBalconyOrientation());
        property.setYearBuilt(request.getYearBuilt());
        property.setPriceAmount(request.getPriceAmount());
        property.setPricePerSquareMeter(pricePerSquareMeter);
        property.setCommissionRate(commissionRate);
        property.setServiceFeeAmount(serviceFeeAmount);
        synchronizeServiceFeeCollection(property);
        property.setAmenities(request.getAmenities());
    }

    private BigDecimal computeServiceFee(BigDecimal priceAmount, BigDecimal commissionRate) {
        if (priceAmount == null || commissionRate == null) {
            return BigDecimal.ZERO;
        }
        return priceAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
    }

    private void addMediaFiles(Property property, MultipartFile[] mediaFiles, List<MediaUploadInfo> mediaMetadata) {
        if (mediaFiles == null || mediaFiles.length == 0) {
            return;
        }

        // Create a map of file index to metadata for quick lookup
        Map<Integer, MediaUploadInfo> metadataMap = new HashMap<>();
        if (mediaMetadata != null) {
            for (MediaUploadInfo info : mediaMetadata) {
                if (info.getFileIndex() != null) {
                    metadataMap.put(info.getFileIndex(), info);
                }
            }
        }

        for (int i = 0; i < mediaFiles.length; i++) {
            MultipartFile mediaFile = mediaFiles[i];
            if (mediaFile == null || mediaFile.isEmpty()) {
                continue;
            }
            try {
                String fileUrl = cloudinaryService.uploadFile(mediaFile, buildMediaFolderPath(property.getId()));
                String mimeType = mediaFile.getContentType() != null ? mediaFile.getContentType() : "application/octet-stream";

                // Get metadata for this file if provided
                MediaUploadInfo metadata = metadataMap.get(i);

                // Determine media type - use metadata if provided, otherwise auto-detect from
                // MIME type
                Constants.MediaTypeEnum mediaType = determineMediaType(mimeType, metadata);

                // Get file name - use metadata if provided, otherwise use original filename
                String fileName = (metadata != null && metadata.getFileName() != null)
                        ? metadata.getFileName()
                        : (mediaFile.getOriginalFilename() != null ? mediaFile.getOriginalFilename()
                                : mediaFile.getName());

                // Get document type description (for DOCUMENT media type)
                String documentType = (metadata != null) ? metadata.getDocumentType() : null;

                Media media = Media.builder()
                        .property(property)
                        .mediaType(mediaType)
                        .fileName(fileName)
                        .filePath(fileUrl)
                        .mimeType(mimeType)
                        .documentType(documentType)
                        .build();
                property.getMediaList().add(media);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to upload property media", e);
            }
        }
    }

    private void removeMediaFiles(Property property, List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty() || property.getMediaList().isEmpty()) {
            return;
        }

        Iterator<Media> iterator = property.getMediaList().iterator();
        while (iterator.hasNext()) {
            Media media = iterator.next();
            if (mediaIds.contains(media.getId())) {
                try {
                    cloudinaryService.deleteFile(media.getFilePath());
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to delete property media", e);
                }
                iterator.remove();
            }
        }
    }

    private void ensureMediaCollection(Property property) {
        if (property.getMediaList() == null) {
            property.setMediaList(new ArrayList<>());
        }
    }

    private String buildMediaFolderPath(UUID propertyId) {
        return "properties/" + propertyId + "/images";
    }

    private void addDocumentFiles(Property property, MultipartFile[] documents,
            List<DocumentUploadInfo> documentMetadata) {
        if (documents == null || documents.length == 0) {
            return;
        }

        // Create a map of file index to metadata for quick lookup
        Map<Integer, DocumentUploadInfo> metadataMap = new HashMap<>();
        if (documentMetadata != null) {
            for (DocumentUploadInfo info : documentMetadata) {
                if (info.getFileIndex() != null) {
                    metadataMap.put(info.getFileIndex(), info);
                }
            }
        }

        // Get or create default document type for files without metadata
        DocumentType defaultDocumentType = getOrCreateDefaultDocumentType();

        for (int i = 0; i < documents.length; i++) {
            MultipartFile documentFile = documents[i];
            if (documentFile == null || documentFile.isEmpty()) {
                continue;
            }
            try {
                String fileUrl = cloudinaryService.uploadFile(documentFile,
                        buildDocumentFolderPath(property.getId()));
                String mimeType = documentFile.getContentType() != null ? documentFile.getContentType()
                        : "application/octet-stream";

                // Get metadata for this file if provided
                DocumentUploadInfo metadata = metadataMap.get(i);

                // Determine document type - use metadata if provided, otherwise use default
                DocumentType documentType;
                if (metadata != null && metadata.getDocumentTypeId() != null) {
                    documentType = documentTypeRepository.findById(metadata.getDocumentTypeId())
                            .orElseThrow(() -> new NotFoundException(
                                    "Document type not found with id: " + metadata.getDocumentTypeId()));
                } else {
                    documentType = defaultDocumentType;
                }

                // Generate document number - use metadata if provided, otherwise generate
                // unique short ID (max 20 chars to fit column constraint)
                String documentNumber = (metadata != null && metadata.getDocumentNumber() != null
                        && !metadata.getDocumentNumber().isBlank())
                                ? metadata.getDocumentNumber().substring(0, Math.min(metadata.getDocumentNumber().length(), 20))
                                : "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                // Get document name - use metadata if provided, otherwise use original filename
                String documentName = (metadata != null && metadata.getDocumentName() != null
                        && !metadata.getDocumentName().isBlank())
                                ? metadata.getDocumentName()
                                : (documentFile.getOriginalFilename() != null ? documentFile.getOriginalFilename()
                                        : documentFile.getName());

                IdentificationDocument document = IdentificationDocument.builder()
                        .documentType(documentType)
                        .property(property)
                        .documentNumber(documentNumber)
                        .documentName(documentName)
                        .filePath(fileUrl)
                        .mimeType(mimeType)
                        .issueDate(metadata != null ? metadata.getIssueDate() : null)
                        .expiryDate(metadata != null ? metadata.getExpiryDate() : null)
                        .issuingAuthority(metadata != null ? metadata.getIssuingAuthority() : null)
                        .verificationStatus(Constants.VerificationStatusEnum.PENDING)
                        .build();

                property.getDocuments().add(document);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to upload property document", e);
            }
        }
    }

    private void removeDocumentFiles(Property property, List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty() || property.getDocuments().isEmpty()) {
            return;
        }

        Iterator<IdentificationDocument> iterator = property.getDocuments().iterator();
        while (iterator.hasNext()) {
            IdentificationDocument document = iterator.next();
            if (documentIds.contains(document.getId())) {
                try {
                    cloudinaryService.deleteFile(document.getFilePath());
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to delete property document", e);
                }
                iterator.remove();
            }
        }
    }

    private void ensureDocumentCollection(Property property) {
        if (property.getDocuments() == null) {
            property.setDocuments(new ArrayList<>());
        }
    }

    private String buildDocumentFolderPath(UUID propertyId) {
        return "properties/" + propertyId + "/documents";
    }

    private DocumentType getOrCreateDefaultDocumentType() {
        // Try to find an existing non-compulsory document type first
        List<DocumentType> nonCompulsoryTypes = documentTypeRepository.findAll().stream()
                .filter(dt -> dt.getIsCompulsory() != null && !dt.getIsCompulsory())
                .toList();

        if (!nonCompulsoryTypes.isEmpty()) {
            return nonCompulsoryTypes.get(0);
        }

        // If no non-compulsory type exists, create a default "General Documents" type
        DocumentType defaultType = DocumentType.builder()
                .name("General Property Documents")
                .description("General documents related to property listings")
                .isCompulsory(false)
                .documents(new ArrayList<>())
                .build();

        return documentTypeRepository.save(defaultType);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Constants.RoleEnum.ADMIN;
    }

    private boolean isPropertyOwner(User user) {
        return user != null && user.getRole() == Constants.RoleEnum.PROPERTY_OWNER;
    }

    private PropertyOwner resolveOwnerForCreate(boolean isAdmin, UUID ownerIdFromRequest, UUID currentUserId) {
        UUID resolvedOwnerId = isAdmin ? ownerIdFromRequest : currentUserId;
        if (resolvedOwnerId == null) {
            throw new IllegalArgumentException("Owner id is required");
        }
        return propertyOwnerRepository.findById(resolvedOwnerId)
                .orElseThrow(() -> new NotFoundException("Property owner not found with id: " + resolvedOwnerId));
    }

    private PropertyOwner resolveOwnerForUpdate(boolean isAdmin, UUID ownerIdFromRequest, PropertyOwner currentOwner) {
        if (!isAdmin) {
            return currentOwner;
        }
        if (ownerIdFromRequest == null || Objects.equals(ownerIdFromRequest, currentOwner.getId())) {
            return currentOwner;
        }
        return propertyOwnerRepository.findById(ownerIdFromRequest)
                .orElseThrow(() -> new NotFoundException("Property owner not found with id: " + ownerIdFromRequest));
    }

    @Override
    public Page<PropertyCard> getFavoritePropertyCards(List<UUID> propertyIds, Pageable pageable) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<PropertyCardProtection> protections = propertyRepository.findFavoritePropertyCards(pageable, propertyIds);
        Page<PropertyCard> cards = propertyMapper.mapToPage(protections, PropertyCard.class);

        cards.forEach(card -> {
            card.setFavorite(true);
            if (card.getOwnerId() != null) {
                card.setOwnerTier(rankingService.getCurrentTier(card.getOwnerId(), Constants.RoleEnum.PROPERTY_OWNER));
            }
            if (card.getAgentId() != null) {
                card.setAgentTier(rankingService.getCurrentTier(card.getAgentId(), Constants.RoleEnum.SALESAGENT));
            }
        });

        return cards;
    }

    @Override
    public List<PropertyContractHistoryDatapoint> getPropertyContractHistory(UUID propertyId, boolean includePastContracts) {
        LocalDate startDate, endDate;
        if (includePastContracts) {
            startDate = LocalDate.now().minusYears(2).minusMonths(6);
            endDate = LocalDate.now().plusYears(2).plusMonths(6);
        } else {
            startDate = LocalDate.now();
            endDate = LocalDate.now().plusYears(5);
        }

        // check if property exists
        propertyRepository.findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Property not found with id: " + propertyId));

        List<Constants.ContractStatusEnum> allowedStatuses = List.of(
            Constants.ContractStatusEnum.ACTIVE,
            Constants.ContractStatusEnum.COMPLETED,
            Constants.ContractStatusEnum.CANCELLED
        );

        // This abomination of code should not exist.
        var datapoints = contractRepository
            .findAllByProperty_IdAndStartDateAfterAndEndDateBeforeAndContractTypeNot(
                propertyId,
                startDate,
                endDate,
                Constants.ContractTypeEnum.DEPOSIT
            ).stream()
            .filter(x -> allowedStatuses.contains(x.getStatus()));

        return datapoints
            .map(c -> {
                if (c.getStatus() == Constants.ContractStatusEnum.CANCELLED) {
                    var a = PropertyContractHistoryDatapoint.builder()
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .status(c.getStatus());
                    if (c.getCancelledAt() != null) {
                        a.endDate(c.getCancelledAt().toLocalDate());
                    }
                    return a.build();
                }
                return PropertyContractHistoryDatapoint.builder()
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .status(c.getStatus())
                        .build();
            })
            .toList();
    }

    /**
     * Validates that all compulsory document types are provided in the document
     * metadata.
     * Admins are exempt from this validation.
     *
     * @param documentMetadata the list of document metadata from the request
     * @param isAdmin          whether the current user is an admin
     * @throws IllegalArgumentException if compulsory documents are missing
     */
    private void validateCompulsoryDocuments(List<DocumentUploadInfo> documentMetadata, boolean isAdmin) {
        // Admins can bypass compulsory document validation
        if (isAdmin) {
            return;
        }

        List<DocumentType> compulsoryDocTypes = documentTypeRepository.findAllByIsCompulsoryTrue();
        if (compulsoryDocTypes.isEmpty()) {
            return; // No compulsory documents required
        }

        Set<UUID> providedDocTypeIds = new HashSet<>();
        if (documentMetadata != null) {
            for (DocumentUploadInfo info : documentMetadata) {
                if (info.getDocumentTypeId() != null) {
                    providedDocTypeIds.add(info.getDocumentTypeId());
                }
            }
        }

        List<String> missingDocTypes = compulsoryDocTypes.stream()
                .filter(dt -> !providedDocTypeIds.contains(dt.getId()))
                .map(DocumentType::getName)
                .collect(Collectors.toList());

        if (!missingDocTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing compulsory documents: " + String.join(", ", missingDocTypes) +
                            ". Please upload all required identification documents.");
        }
    }

    /**
     * Validates that a property has all compulsory document types after an update.
     * This checks the actual documents attached to the property entity.
     *
     * @param property the property to validate
     * @param isAdmin  whether the current user is an admin
     * @throws IllegalArgumentException if compulsory documents are missing
     */
    private void validatePropertyHasCompulsoryDocuments(Property property, boolean isAdmin) {
        // Admins can bypass compulsory document validation
        if (isAdmin) {
            return;
        }

        List<DocumentType> compulsoryDocTypes = documentTypeRepository.findAllByIsCompulsoryTrue();
        if (compulsoryDocTypes.isEmpty()) {
            return; // No compulsory documents required
        }

        Set<UUID> existingDocTypeIds = property.getDocuments().stream()
                .map(doc -> doc.getDocumentType().getId())
                .collect(Collectors.toSet());

        List<String> missingDocTypes = compulsoryDocTypes.stream()
                .filter(dt -> !existingDocTypeIds.contains(dt.getId()))
                .map(DocumentType::getName)
                .collect(Collectors.toList());

        if (!missingDocTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Property is missing compulsory documents: " + String.join(", ", missingDocTypes) +
                            ". Cannot update property without all required identification documents.");
        }
    }

    /**
     * Determines the media type based on the MIME type and optional metadata.
     *
     * @param mimeType the MIME type of the file
     * @param metadata optional metadata that may specify the media type
     * @return the determined MediaTypeEnum
     */
    private Constants.MediaTypeEnum determineMediaType(String mimeType, MediaUploadInfo metadata) {
        // If metadata explicitly specifies media type, use it
        if (metadata != null && metadata.getMediaType() != null) {
            return metadata.getMediaType();
        }

        // Auto-detect from MIME type
        if (mimeType == null) {
            return Constants.MediaTypeEnum.IMAGE; // Default to IMAGE
        }

        String lowerMimeType = mimeType.toLowerCase();
        if (lowerMimeType.startsWith("image/")) {
            return Constants.MediaTypeEnum.IMAGE;
        } else if (lowerMimeType.startsWith("video/")) {
            return Constants.MediaTypeEnum.VIDEO;
        } else {
            // PDFs, docs, and other files are treated as documents
            return Constants.MediaTypeEnum.DOCUMENT;
        }
    }
}
