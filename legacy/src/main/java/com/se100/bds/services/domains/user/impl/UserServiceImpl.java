package com.se100.bds.services.domains.user.impl;

import com.se100.bds.dtos.requests.account.UpdateAccountDto;
import com.se100.bds.dtos.requests.auth.RegisterRequest;
import com.se100.bds.dtos.responses.user.listitem.CustomerListItem;
import com.se100.bds.dtos.responses.user.listitem.FreeAgentListItem;
import com.se100.bds.dtos.responses.user.listitem.PropertyOwnerListItem;
import com.se100.bds.dtos.responses.user.listitem.SaleAgentListItem;
import com.se100.bds.dtos.responses.user.meprofile.MeResponse;
import com.se100.bds.dtos.responses.user.propertyprofile.CustomerPropertyProfileResponse;
import com.se100.bds.dtos.responses.user.propertyprofile.PropertyOwnerPropertyProfileResponse;
import com.se100.bds.dtos.responses.user.otherprofile.UserProfileResponse;
import com.se100.bds.mappers.UserMapper;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialAll;
import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialMonth;
import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionAll;
import com.se100.bds.models.schemas.ranking.IndividualPropertyOwnerContributionMonth;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.user.PropertyOwnerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.repositories.domains.user.UserRepository;
import com.se100.bds.securities.JwtUserDetails;
import com.se100.bds.services.MessageSourceService;
import com.se100.bds.services.domains.appointment.AppointmentService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.ranking.RankingService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.fileupload.CloudinaryService;
import com.se100.bds.utils.Constants;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;

    private final PasswordEncoder passwordEncoder;
    private final MessageSourceService messageSourceService;
    private final UserMapper userMapper;
    private final PropertyService propertyService;
    private final AppointmentService appointmentService;
    private final RankingService rankingService;
    private final WardRepository wardRepository;
    private final CloudinaryService cloudinaryService;

    public UserServiceImpl(
            UserRepository userRepository,
            SaleAgentRepository saleAgentRepository,
            PropertyOwnerRepository propertyOwnerRepository,
            PasswordEncoder passwordEncoder,
            MessageSourceService messageSourceService,
            UserMapper userMapper,
            @Lazy PropertyService propertyService,
            @Lazy AppointmentService appointmentService,
            @Lazy RankingService rankingService,
            CloudinaryService cloudinaryService,
            WardRepository wardRepository
    ) {
        this.userRepository = userRepository;
        this.saleAgentRepository = saleAgentRepository;
        this.propertyOwnerRepository = propertyOwnerRepository;
        this.passwordEncoder = passwordEncoder;
        this.messageSourceService = messageSourceService;
        this.userMapper = userMapper;
        this.propertyService = propertyService;
        this.appointmentService = appointmentService;
        this.cloudinaryService = cloudinaryService;
        this.rankingService = rankingService;
        this.wardRepository = wardRepository;
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser() {
        Authentication authentication = getAuthentication();
        if (authentication.isAuthenticated()) {
            try {
                return findById(UUID.fromString(getPrincipal(authentication).getId()));
            } catch (ClassCastException e) {
                log.warn("[JWT] User details not found!");
                throw new BadCredentialsException("Bad credentials");
            }
        } else {
            log.warn("[JWT] User not authenticated!");
            throw new BadCredentialsException("Bad credentials");
        }
    }

    @Override
    @Transactional
    public void approveAccount(UUID propOwnerId, boolean approve) {
        try {
            User user = findById(propOwnerId);
            if (user.getStatus() == Constants.StatusProfileEnum.PENDING_APPROVAL) {
                if (approve) {
                    user.setStatus(Constants.StatusProfileEnum.ACTIVE);
                    PropertyOwner propertyOwner = propertyOwnerRepository.findById(propOwnerId).orElseThrow(
                            () -> new EntityNotFoundException("User not found with id: " + propOwnerId)
                    );
                    propertyOwner.setApprovedAt(LocalDateTime.now());
                    propertyOwnerRepository.save(propertyOwner);
                }
                else {
                    user.setStatus(Constants.StatusProfileEnum.REJECTED);
                    PropertyOwner propertyOwner = propertyOwnerRepository.findById(propOwnerId).orElseThrow(
                            () -> new EntityNotFoundException("User not found with id: " + propOwnerId)
                    );
                    propertyOwner.setApprovedAt(null);
                    propertyOwnerRepository.save(propertyOwner);
                }
                userRepository.save(user);
            }
        } catch (Exception exception) {
            log.warn("[JWT] User details not found!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllByName(String name) {
        return Objects.equals(name, "") || name == null ? userRepository.findAll() : userRepository.findAllByFullNameIsLikeIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse<?> getAccount() {
        Authentication authentication = getAuthentication();
        if (authentication.isAuthenticated()) {
            try {
                UUID userId = UUID.fromString(getPrincipal(authentication).getId());
                return getUserById(userId);
            } catch (ClassCastException e) {
                log.warn("[JWT] User details not found!");
                throw new BadCredentialsException("Bad credentials");
            }
        } else {
            log.warn("[JWT] User not authenticated!");
            throw new BadCredentialsException("Bad credentials");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse<?> getUserById(UUID userId) {
        User user = userRepository.findByIdWithLocation(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        @SuppressWarnings("unchecked")
        MeResponse<Object> meResponse = userMapper.mapTo(user, MeResponse.class);
        LocalDateTime now = LocalDateTime.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        meResponse.setTier(rankingService.getTier(user.getId(), user.getRole(), month, year));

        switch (user.getRole()) {
            case ADMIN -> {
                return null;
            }
            case CUSTOMER -> {
                List<Property> properties = propertyService.getAllByUserIdAndStatus(null, user.getId(), null, null);
                int bought = 0;
                int rent = 0;
                int total = properties.size();

                for (Property property : properties) {
                   if (property.getTransactionType() == Constants.TransactionTypeEnum.SALE) {
                       bought++;
                   }
                   if (property.getTransactionType() == Constants.TransactionTypeEnum.RENTAL) {
                       rent++;
                   }
                }

                meResponse.setProfile(
                        CustomerPropertyProfileResponse.builder()
                                .totalListings(total)
                                .totalBought(bought)
                                .totalRented(rent)
                                .build()
                );

                // Get month statistics
                IndividualCustomerPotentialMonth customerPotentialMonth = rankingService.getCustomerMonth(user.getId(), month, year);
                meResponse.setStatisticMonth(customerPotentialMonth);

                // Get all-time statistics
                IndividualCustomerPotentialAll customerPotentialAll = rankingService.getCustomerAll(user.getId());
                meResponse.setStatisticAll(customerPotentialAll);
            }
            case SALESAGENT -> {
                // Get month statistics
                IndividualSalesAgentPerformanceMonth agentPerformanceMonth = rankingService.getSaleAgentMonth(user.getId(), month, year);
                meResponse.setStatisticMonth(agentPerformanceMonth);

                // Get all-time statistics
                IndividualSalesAgentPerformanceCareer agentPerformanceCareer = rankingService.getSaleAgentCareer(user.getId());
                meResponse.setStatisticAll(agentPerformanceCareer);
            }
            case PROPERTY_OWNER -> {
                List<Property> properties = propertyService.getAllByUserIdAndStatus(user.getId(), null, null, null);
                int totalSolds = 0;
                int totalRentals = 0;

                for (Property property : properties) {
                    Constants.TransactionTypeEnum transactionType = property.getTransactionType();

                    if (transactionType == Constants.TransactionTypeEnum.SALE) {
                        totalSolds++;
                    }
                    else if (transactionType == Constants.TransactionTypeEnum.RENTAL) {
                        totalRentals++;
                    }
                }

                meResponse.setProfile(
                        PropertyOwnerPropertyProfileResponse.builder()
                                .totalListings(properties.size())
                                .totalSolds(totalSolds)
                                .totalRentals(totalRentals)
                                .build()
                );

                // Get month statistics
                IndividualPropertyOwnerContributionMonth ownerContributionMonth = rankingService.getPropertyOwnerMonth(user.getId(), month, year);
                meResponse.setStatisticMonth(ownerContributionMonth);

                // Get all-time statistics
                IndividualPropertyOwnerContributionAll ownerContributionAll = rankingService.getPropertyOwnerAll(user.getId());
                meResponse.setStatisticAll(ownerContributionAll);
            }
            default -> throw new BadCredentialsException("Bad credentials");
        }

        return meResponse;
    }

    @Override
    @Transactional
    public MeResponse<?> updateUserById(UUID userId, UpdateAccountDto updateAccountDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Update fields only if they are not null
        if (updateAccountDto.getFirstName() != null) {
            user.setFirstName(updateAccountDto.getFirstName());
        }
        if (updateAccountDto.getLastName() != null) {
            user.setLastName(updateAccountDto.getLastName());
        }
        if (updateAccountDto.getPhoneNumber() != null) {
            user.setPhoneNumber(updateAccountDto.getPhoneNumber());
        }
        if (updateAccountDto.getEmail() != null) {
            user.setEmail(updateAccountDto.getEmail());
        }
        if (updateAccountDto.getDayOfBirth() != null) {
            user.setDayOfBirth(updateAccountDto.getDayOfBirth());
        }
        if (updateAccountDto.getGender() != null) {
            user.setGender(updateAccountDto.getGender());
        }
        if (updateAccountDto.getNation() != null) {
            user.setNation(updateAccountDto.getNation());
        }
        if (updateAccountDto.getIdentificationNumber() != null) {
            user.setIdentificationNumber(updateAccountDto.getIdentificationNumber());
        }
        if (updateAccountDto.getIssuedDate() != null) {
            user.setIssueDate(updateAccountDto.getIssuedDate());
        }
        if (updateAccountDto.getIssuingAuthority() != null) {
            user.setIssuingAuthority(updateAccountDto.getIssuingAuthority());
        }
        if (updateAccountDto.getZaloContract() != null) {
            user.setZaloContact(updateAccountDto.getZaloContract());
        }
        if (updateAccountDto.getWardId() != null) {
            user.setWard(wardRepository.findById(updateAccountDto.getWardId())
                    .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + updateAccountDto.getWardId())));
        }

        if (updateAccountDto.getAvatar() != null && !updateAccountDto.getAvatar().isEmpty()) {
            try {
                // Delete old avatar if exists
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    cloudinaryService.deleteFile(user.getAvatarUrl());
                }
                // Upload new avatar
                String avatarUrl = cloudinaryService.uploadFile(updateAccountDto.getAvatar(), "avatars");
                user.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                log.error("Failed to upload avatar: {}", e.getMessage());
                throw new RuntimeException("Failed to upload avatar");
            }
        }

        if (updateAccountDto.getFrontIdPicture() != null && !updateAccountDto.getFrontIdPicture().isEmpty()) {
            try {
                // Delete old front ID picture if exists
                if (user.getFrontIdPicturePath() != null && !user.getFrontIdPicturePath().isEmpty()) {
                    cloudinaryService.deleteFile(user.getFrontIdPicturePath());
                }
                // Upload new front ID picture
                String frontIdUrl = cloudinaryService.uploadFile(updateAccountDto.getFrontIdPicture(), "id_pictures");
                user.setFrontIdPicturePath(frontIdUrl);
            } catch (IOException e) {
                log.error("Failed to upload front ID picture: {}", e.getMessage());
                throw new RuntimeException("Failed to upload front ID picture");
            }
        }

        if (updateAccountDto.getBackIdPicture() != null && !updateAccountDto.getBackIdPicture().isEmpty()) {
            try {
                // Delete old back ID picture if exists
                if (user.getBackIdPicturePath() != null && !user.getBackIdPicturePath().isEmpty()) {
                    cloudinaryService.deleteFile(user.getBackIdPicturePath());
                }
                // Upload new back ID picture
                String backIdUrl = cloudinaryService.uploadFile(updateAccountDto.getBackIdPicture(), "id_pictures");
                user.setBackIdPicturePath(backIdUrl);
            } catch (IOException e) {
                log.error("Failed to upload back ID picture: {}", e.getMessage());
                throw new RuntimeException("Failed to upload back ID picture");
            }
        }

        // Save updated user
        userRepository.save(user);

        // Return updated MeResponse
        return getUserById(userId);
    }

    @Override
    @Transactional
    public MeResponse<?> updateMe(UpdateAccountDto updateAccountDto) {
        Authentication authentication = getAuthentication();
        if (authentication.isAuthenticated()) {
            try {
                UUID userId = UUID.fromString(getPrincipal(authentication).getId());
                return updateUserById(userId, updateAccountDto);
            } catch (ClassCastException e) {
                log.warn("[JWT] User details not found!");
                throw new BadCredentialsException("Bad credentials");
            }
        } else {
            log.warn("[JWT] User not authenticated!");
            throw new BadCredentialsException("Bad credentials");
        }
    }

    @Override
    @Transactional
    public void deleteAccountById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getRole() == Constants.RoleEnum.ADMIN) {
            log.info("Hard deleting ADMIN user with id: {}", userId);
            userRepository.delete(user);
        } else {
            log.info("Soft deleting user with id: {}, role: {}", userId, user.getRole());
            user.setStatus(Constants.StatusProfileEnum.DELETED);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void deleteMyAccount() {
        Authentication authentication = getAuthentication();
        if (authentication.isAuthenticated()) {
            try {
                UUID userId = UUID.fromString(getPrincipal(authentication).getId());
                deleteAccountById(userId);
            } catch (ClassCastException e) {
                log.warn("[JWT] User details not found!");
                throw new BadCredentialsException("Bad credentials");
            }
        } else {
            log.warn("[JWT] User not authenticated!");
            throw new BadCredentialsException("Bad credentials");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getUserId() {
        return getUser().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAllWithLocation(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public @NotNull User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public SaleAgent findSaleAgentById(UUID agentId) {
        return saleAgentRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + agentId));
    }

    @Override
    @Transactional
    public User updateStatus(UUID id, String status) {
        User user = findById(id);
        try {
            user.setStatus(Constants.StatusProfileEnum.valueOf(status.toUpperCase()));
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(messageSourceService.get("invalid_status"));
        }
    }

    /**
     * Find a user by email.
     *
     * @param email String.
     * @return User
     */
    @Transactional(readOnly = true)
    public User findByEmail(final String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("user")})));
    }

    /**
     * Load user details by username.
     *
     * @param email String
     * @return UserDetails
     * @throws UsernameNotFoundException email not found exception.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(final String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("user")})));

        return JwtUserDetails.create(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse<?> getUserProfileById(UUID id) {
        User user = userRepository.findByIdWithLocation(id)
                .orElseThrow(() -> new NotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("user")})));

        UserProfileResponse<?> userProfileResponse = userMapper.mapTo(user, UserProfileResponse.class);
        userProfileResponse.setWardId(user.getWard().getId());
        userProfileResponse.setWardName(user.getWard().getWardName());
        userProfileResponse.setDistrictId(user.getWard().getDistrict().getId());
        userProfileResponse.setDistrictName(user.getWard().getDistrict().getDistrictName());
        userProfileResponse.setCityId(user.getWard().getDistrict().getCity().getId());
        userProfileResponse.setCityName(user.getWard().getDistrict().getCity().getCityName());

        switch (user.getRole()) {
            case ADMIN -> {
                return userProfileResponse;
            }
            case CUSTOMER -> {
                userProfileResponse.setTier(rankingService.getCurrentTier(id, Constants.RoleEnum.CUSTOMER));

                CustomerPropertyProfileResponse customerPropertyProfileResponse = new CustomerPropertyProfileResponse();
                List<Property> properties = propertyService.getAllByUserIdAndStatus(null, id, null, null);

                int totalSolds = 0;
                int totalRentals = 0;

                for (Property property : properties) {
                    if (property.getTransactionType() == Constants.TransactionTypeEnum.SALE) {
                        totalSolds++;
                    }
                    else  if (property.getTransactionType() == Constants.TransactionTypeEnum.RENTAL) {
                        totalRentals++;
                    }
                }

                customerPropertyProfileResponse.setTotalListings(properties.size());
                customerPropertyProfileResponse.setTotalBought(totalSolds);
                customerPropertyProfileResponse.setTotalRented(totalRentals);

                UserProfileResponse<CustomerPropertyProfileResponse> customerProfileResponse =
                        (UserProfileResponse<CustomerPropertyProfileResponse>) userProfileResponse;

                customerProfileResponse.setPropertyProfile(customerPropertyProfileResponse);
                return customerProfileResponse;
            }
            case SALESAGENT -> {
                userProfileResponse.setTier(rankingService.getCurrentTier(id, Constants.RoleEnum.SALESAGENT));

                return userProfileResponse;
            }
            case PROPERTY_OWNER -> {
                userProfileResponse.setTier(rankingService.getCurrentTier(id, Constants.RoleEnum.PROPERTY_OWNER));

                PropertyOwnerPropertyProfileResponse ownerPropertyProfileResponse = new PropertyOwnerPropertyProfileResponse();
                List<Property> properties = propertyService.getAllByUserIdAndStatus(id, null, null,
                                List.of(Constants.PropertyStatusEnum.AVAILABLE, Constants.PropertyStatusEnum.SOLD, Constants.PropertyStatusEnum.RENTED));

                int totalSolds = 0;
                int totalRentals = 0;

                for (Property property : properties) {
                    Constants.TransactionTypeEnum transactionType = property.getTransactionType();
                    Constants.PropertyStatusEnum status = property.getStatus();

                    // Count SALE transactions that are SOLD or AVAILABLE
                    if (transactionType == Constants.TransactionTypeEnum.SALE
                            && (status == Constants.PropertyStatusEnum.SOLD || status == Constants.PropertyStatusEnum.AVAILABLE)) {
                        totalSolds++;
                    }
                    // Count RENTAL transactions that are RENTED or AVAILABLE
                    else if (transactionType == Constants.TransactionTypeEnum.RENTAL
                            && (status == Constants.PropertyStatusEnum.RENTED || status == Constants.PropertyStatusEnum.AVAILABLE)) {
                        totalRentals++;
                    }
                }

                ownerPropertyProfileResponse.setTotalListings(properties.size());
                ownerPropertyProfileResponse.setTotalSolds(totalSolds);
                ownerPropertyProfileResponse.setTotalRentals(totalRentals);

                UserProfileResponse<PropertyOwnerPropertyProfileResponse> ownerProfileResponse =
                        (UserProfileResponse<PropertyOwnerPropertyProfileResponse>) userProfileResponse;
                ownerProfileResponse.setPropertyProfile(ownerPropertyProfileResponse);
                return ownerProfileResponse;
            }
            default -> throw new BadCredentialsException(messageSourceService.get("invalid_role"));
        }
    }

    /**
     * Loads user details by UUID string.
     *
     * @param id String
     * @return UserDetails
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserById(final String id) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("user")})));

        return JwtUserDetails.create(user);
    }

    /**
     * Get UserDetails from security context.
     *
     * @param authentication Wrapper for security context
     * @return the Principal being authenticated or the authenticated principal after authentication.
     */
    @Override
    @Transactional(readOnly = true)
    public JwtUserDetails getPrincipal(final Authentication authentication) {
        return (JwtUserDetails) authentication.getPrincipal();
    }

    @Override
    @Transactional
    public User register(final RegisterRequest request, Constants.RoleEnum roleEnum) throws BindException, IOException {
        if (userRepository.existsByEmail(request.getEmail())) {
            BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), "email",
                    "Email already exists"));
            throw new BindException(bindingResult);
        }

        Ward ward = null;
        if (request.getWardId() != null) {
            ward = wardRepository.findById(request.getWardId())
                    .orElseThrow(() -> new NotFoundException(messageSourceService.get("not_found_with_param",
                            new String[]{"Ward"})));
        }

        User user = userMapper.mapTo(request, User.class);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setWard(ward);
        user.setFrontIdPicturePath(cloudinaryService.uploadFile(request.getFrontIdPicture(), "Identification Picture"));
        user.setBackIdPicturePath(cloudinaryService.uploadFile(request.getBackIdPicture(), "Identification Picture"));

        if (roleEnum.equals(Constants.RoleEnum.CUSTOMER)) {
            user.setStatus(Constants.StatusProfileEnum.ACTIVE);
            user.setRole(Constants.RoleEnum.CUSTOMER);

            Customer customer = new Customer();
            customer.setUser(user);
            user.setCustomer(customer);
        } else {
            user.setStatus(Constants.StatusProfileEnum.PENDING_APPROVAL);
            user.setRole(Constants.RoleEnum.PROPERTY_OWNER);

            PropertyOwner propertyOwner = new PropertyOwner();
            propertyOwner.setUser(user);
            user.setPropertyOwner(propertyOwner);
        }

        userRepository.save(user);

        return user;
    }


    @Transactional
    @Override
    public void delete(String id) {
        userRepository.delete(findById(UUID.fromString(id)));
    }

    @Override
    @Transactional
    public void activeteUser(String id) {
        if (!userRepository.existsById(UUID.fromString(id))) {
            throw new NotFoundException(messageSourceService.get("not_found_with_param",
                    new String[]{messageSourceService.get("user")}));
        }
        User user = findById(UUID.fromString(id));
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleAgentListItem> getAllSaleAgentItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<Constants.PerformanceTierEnum> agentTiers, Integer maxProperties,
            Integer minPerformancePoint, Integer maxPerformancePoint,
            Integer minRanking, Integer maxRanking,
            Integer minAssignments, Integer maxAssignments,
            Integer minAssignedProperties, Integer maxAssignedProperties,
            Integer minAssignedAppointments, Integer maxAssignedAppointments,
            Integer minContracts, Integer maxContracts,
            Double minAvgRating, Double maxAvgRating,
            LocalDateTime hiredDateFrom, LocalDateTime hiredDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    ) {
        List<User> agents = userRepository.findAllSaleAgentWithFiltersTestHiredDate(
                name,
                maxProperties,
                cityIds, districtIds, wardIds
        );

        List<SaleAgentListItem> agentListItemList = new ArrayList<>();

        boolean findByMonth = false;
        if (month != null) findByMonth = true;

        for (User agentUser : agents) {
            LocalDateTime agentHiredDate = agentUser.getSaleAgent().getHiredDate();
            if (hiredDateFrom != null && agentHiredDate.isBefore(hiredDateFrom)) {
                continue;
            }
            if (hiredDateTo != null && agentHiredDate.isAfter(hiredDateTo)) {
                continue;
            }

            if (findByMonth) {
                IndividualSalesAgentPerformanceMonth agentPerformanceMonth = rankingService.getSaleAgentMonth(
                        agentUser.getId(),
                        month,
                        year
                );

                if (agentTiers != null && !agentTiers.isEmpty()) {
                    boolean found = false;
                    for (var tier : agentTiers) {
                        if (agentPerformanceMonth.getPerformanceTier() == tier) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }

                Integer performancePoint = agentPerformanceMonth.getPerformancePoint();
                if (minPerformancePoint != null && performancePoint < minPerformancePoint) {
                    break;
                }
                if (maxPerformancePoint != null && performancePoint > maxPerformancePoint) {
                    break;
                }

                Integer ranking = agentPerformanceMonth.getRankingPosition();
                if (minRanking != null && ranking < minRanking) {
                    break;
                }
                if (maxRanking != null && ranking > maxRanking) {
                    break;
                }

                Integer assignedProperties = agentPerformanceMonth.getMonthPropertiesAssigned();
                if (minAssignedProperties != null && assignedProperties < minAssignedProperties) {
                    break;
                }
                if (maxAssignedProperties != null && assignedProperties > maxAssignedProperties) {
                    break;
                }

                Integer assignedAppointments = agentPerformanceMonth.getMonthAppointmentsAssigned();
                if  (minAssignedAppointments != null && assignedAppointments < minAssignedAppointments) {
                    break;
                }
                if (maxAssignedAppointments != null && assignedAppointments > maxAssignedAppointments) {
                    break;
                }

                int assignments = assignedProperties + assignedAppointments;
                if (minAssignments != null && assignments < minAssignments) {
                    break;
                }
                if (maxAssignments != null && assignments > maxAssignments) {
                    break;
                }

                Integer monthContracts = agentPerformanceMonth.getMonthContracts();
                if (minContracts != null && monthContracts < minContracts) {
                    break;
                }
                if (maxContracts != null && monthContracts > maxContracts) {
                    break;
                }

                BigDecimal avgRating = agentPerformanceMonth.getAvgRating();
                if (minAvgRating != null && avgRating.compareTo(BigDecimal.valueOf(minAvgRating)) > 0) {
                    break;
                }
                if (maxAvgRating != null && avgRating.compareTo(BigDecimal.valueOf(maxAvgRating)) < 0) {
                    break;
                }

                agentListItemList.add(
                        SaleAgentListItem.builder()
                                .id(agentUser.getId())
                                .createdAt(agentUser.getCreatedAt())
                                .updatedAt(agentUser.getUpdatedAt())
                                .firstName(agentUser.getFirstName())
                                .lastName(agentUser.getLastName())
                                .avatarUrl(agentUser.getAvatarUrl())
                                .ranking(ranking)
                                .employeeCode(agentUser.getSaleAgent().getEmployeeCode())
                                .point(performancePoint)
                                .tier(agentPerformanceMonth.getPerformanceTier().getValue())
                                .totalAssignments(assignments)
                                .propertiesAssigned(assignedProperties)
                                .appointmentsAssigned(assignedAppointments)
                                .totalContracts(monthContracts)
                                .rating(avgRating.doubleValue())
                                .totalRates(agentPerformanceMonth.getMonthRates())
                                .hiredDate(agentUser.getSaleAgent().getHiredDate())
                                .location(agentUser.getWard().getWardName() + ", " +
                                         agentUser.getWard().getDistrict().getDistrictName() + ", " +
                                         agentUser.getWard().getDistrict().getCity().getCityName())
                                .build()
                );
            } else {
                IndividualSalesAgentPerformanceCareer agentPerformanceCareer = rankingService.getSaleAgentCareer(agentUser.getId());

                Integer performancePoint = agentPerformanceCareer.getPerformancePoint();
                if (minPerformancePoint != null && performancePoint < minPerformancePoint) {
                    continue;
                }
                if (maxPerformancePoint != null && performancePoint > maxPerformancePoint) {
                    continue;
                }

                Integer ranking = agentPerformanceCareer.getCareerRanking();
                if (minRanking != null && ranking < minRanking) {
                    continue;
                }
                if (maxRanking != null && ranking > maxRanking) {
                    continue;
                }

                Integer assignedProperties = agentPerformanceCareer.getPropertiesAssigned();
                if (minAssignedProperties != null && assignedProperties < minAssignedProperties) {
                    continue;
                }
                if (maxAssignedProperties != null && assignedProperties > maxAssignedProperties) {
                    continue;
                }

                Integer assignedAppointments = agentPerformanceCareer.getAppointmentAssigned();
                if (minAssignedAppointments != null && assignedAppointments < minAssignedAppointments) {
                    continue;
                }
                if (maxAssignedAppointments != null && assignedAppointments > maxAssignedAppointments) {
                    continue;
                }

                int assignments = assignedProperties + assignedAppointments;
                if (minAssignments != null && assignments < minAssignments) {
                    continue;
                }
                if (maxAssignments != null && assignments > maxAssignments) {
                    continue;
                }

                Integer totalContracts = agentPerformanceCareer.getTotalContracts();
                if (minContracts != null && totalContracts < minContracts) {
                    continue;
                }
                if (maxContracts != null && totalContracts > maxContracts) {
                    continue;
                }

                BigDecimal avgRating = agentPerformanceCareer.getAvgRating();
                if (minAvgRating != null && avgRating.compareTo(BigDecimal.valueOf(minAvgRating)) < 0) {
                    continue;
                }
                if (maxAvgRating != null && avgRating.compareTo(BigDecimal.valueOf(maxAvgRating)) > 0) {
                    continue;
                }

                agentListItemList.add(
                        SaleAgentListItem.builder()
                                .id(agentUser.getId())
                                .createdAt(agentUser.getCreatedAt())
                                .updatedAt(agentUser.getUpdatedAt())
                                .firstName(agentUser.getFirstName())
                                .lastName(agentUser.getLastName())
                                .avatarUrl(agentUser.getAvatarUrl())
                                .ranking(ranking)
                                .employeeCode(agentUser.getSaleAgent().getEmployeeCode())
                                .point(performancePoint)
                                .tier(null)
                                .totalAssignments(assignments)
                                .propertiesAssigned(assignedProperties)
                                .appointmentsAssigned(assignedAppointments)
                                .totalContracts(totalContracts)
                                .rating(avgRating.doubleValue())
                                .totalRates(agentPerformanceCareer.getTotalRates())
                                .hiredDate(agentUser.getSaleAgent().getHiredDate())
                                .location(agentUser.getWard().getWardName() + ", " +
                                         agentUser.getWard().getDistrict().getDistrictName() + ", " +
                                         agentUser.getWard().getDistrict().getCity().getCityName())
                                .build()
                );
            }
        }

        agentListItemList = agentListItemList.stream()
                .sorted(Comparator.comparing(
                        SaleAgentListItem::getPoint,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();


        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), agentListItemList.size());
        List<SaleAgentListItem> pageContent = agentListItemList.subList(start, end);

        return new PageImpl<>(pageContent, pageable, agentListItemList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerListItem> getAllCustomerItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<Constants.CustomerTierEnum> customerTierEnums,
            Integer minLeadingScore, Integer maxLeadingScore,
            Integer minViewings, Integer maxViewings,
            BigDecimal minSpending, BigDecimal maxSpending,
            Integer minContracts, Integer maxContracts,
            Integer minPropertiesBought, Integer maxPropertiesBought,
            Integer minPropertiesRented, Integer maxPropertiesRented,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    ) {
        List<User> customers = userRepository.findAllByCustomerFullNameIsLikeIgnoreCaseAndRangeJoinedDateAndLocation(
                name,
                cityIds, districtIds, wardIds
        );

        List<CustomerListItem> customerListItemList = new ArrayList<>();

        boolean findByMonth = month != null;

        for (User customerUser : customers) {
            // Filter by joinedDate (createdAt) in service layer
            LocalDateTime customerJoinedDate = customerUser.getCreatedAt();
            if (joinedDateFrom != null && customerJoinedDate.isBefore(joinedDateFrom)) {
                continue;
            }
            if (joinedDateTo != null && customerJoinedDate.isAfter(joinedDateTo)) {
                continue;
            }

            if (findByMonth) {
                IndividualCustomerPotentialMonth customerPotentialMonth = rankingService.getCustomerMonth(
                        customerUser.getId(),
                        month,
                        year
                );

                // Filter by customer tiers if provided
                if (customerTierEnums != null && !customerTierEnums.isEmpty()) {
                    boolean found = false;
                    for (var tier : customerTierEnums) {
                        if (customerPotentialMonth.getCustomerTier() == tier) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }

                Integer leadScore = customerPotentialMonth.getLeadScore();
                if (minLeadingScore != null && leadScore < minLeadingScore) {
                    continue;
                }
                if (maxLeadingScore != null && leadScore > maxLeadingScore) {
                    continue;
                }

                Integer ranking = customerPotentialMonth.getLeadPosition();
                if (minRanking != null && ranking < minRanking) {
                    continue;
                }
                if (maxRanking != null && ranking > maxRanking) {
                    continue;
                }

                Integer viewingsRequested = customerPotentialMonth.getMonthViewingsRequested();
                if (minViewings != null && viewingsRequested < minViewings) {
                    continue;
                }
                if (maxViewings != null && viewingsRequested > maxViewings) {
                    continue;
                }

                BigDecimal spending = customerPotentialMonth.getMonthSpending();
                if (minSpending != null && spending.compareTo(minSpending) < 0) {
                    continue;
                }
                if (maxSpending != null && spending.compareTo(maxSpending) > 0) {
                    continue;
                }

                Integer monthContracts = customerPotentialMonth.getMonthContractsSigned();
                if (minContracts != null && monthContracts < minContracts) {
                    continue;
                }
                if (maxContracts != null && monthContracts > maxContracts) {
                    continue;
                }

                Integer purchases = customerPotentialMonth.getMonthPurchases();
                if (minPropertiesBought != null && purchases < minPropertiesBought) {
                    continue;
                }
                if (maxPropertiesBought != null && purchases > maxPropertiesBought) {
                    continue;
                }

                Integer rentals = customerPotentialMonth.getMonthRentals();
                if (minPropertiesRented != null && rentals < minPropertiesRented) {
                    continue;
                }
                if (maxPropertiesRented != null && rentals > maxPropertiesRented) {
                    continue;
                }

                customerListItemList.add(
                        CustomerListItem.builder()
                                .id(customerUser.getId())
                                .firstName(customerUser.getFirstName())
                                .lastName(customerUser.getLastName())
                                .avatarUrl(customerUser.getAvatarUrl())
                                .ranking(ranking)
                                .point(leadScore)
                                .tier(customerPotentialMonth.getCustomerTier().getValue())
                                .totalSpending(spending)
                                .totalViewings(viewingsRequested)
                                .totalContracts(monthContracts)
                                .createdAt(customerUser.getCreatedAt())
                                .updatedAt(customerUser.getUpdatedAt())
                                .location(customerUser.getWard().getWardName() + ", " +
                                         customerUser.getWard().getDistrict().getDistrictName() + ", " +
                                         customerUser.getWard().getDistrict().getCity().getCityName())
                                .build()
                );
            } else {
                IndividualCustomerPotentialAll customerPotentialAll = rankingService.getCustomerAll(customerUser.getId());

                Integer leadScore = customerPotentialAll.getLeadScore();
                if (minLeadingScore != null && leadScore < minLeadingScore) {
                    continue;
                }
                if (maxLeadingScore != null && leadScore > maxLeadingScore) {
                    continue;
                }

                // leadPosition is String in All schema, need to parse it
                Integer ranking = null;
                try {
                    ranking = customerPotentialAll.getLeadPosition();
                } catch (NumberFormatException e) {
                    // If parsing fails, skip ranking filter
                }
                if (ranking != null) {
                    if (minRanking != null && ranking < minRanking) {
                        continue;
                    }
                    if (maxRanking != null && ranking > maxRanking) {
                        continue;
                    }
                }

                Integer viewingsRequested = customerPotentialAll.getViewingsRequested();
                if (minViewings != null && viewingsRequested < minViewings) {
                    continue;
                }
                if (maxViewings != null && viewingsRequested > maxViewings) {
                    continue;
                }

                BigDecimal spending = customerPotentialAll.getSpending();
                if (minSpending != null && spending.compareTo(minSpending) < 0) {
                    continue;
                }
                if (maxSpending != null && spending.compareTo(maxSpending) > 0) {
                    continue;
                }

                Integer totalContracts = customerPotentialAll.getTotalContractsSigned();
                if (minContracts != null && totalContracts < minContracts) {
                    continue;
                }
                if (maxContracts != null && totalContracts > maxContracts) {
                    continue;
                }

                Integer purchases = customerPotentialAll.getTotalPurchases();
                if (minPropertiesBought != null && purchases < minPropertiesBought) {
                    continue;
                }
                if (maxPropertiesBought != null && purchases > maxPropertiesBought) {
                    continue;
                }

                Integer rentals = customerPotentialAll.getTotalRentals();
                if (minPropertiesRented != null && rentals < minPropertiesRented) {
                    continue;
                }
                if (maxPropertiesRented != null && rentals > maxPropertiesRented) {
                    continue;
                }

                customerListItemList.add(
                        CustomerListItem.builder()
                                .id(customerUser.getId())
                                .firstName(customerUser.getFirstName())
                                .lastName(customerUser.getLastName())
                                .avatarUrl(customerUser.getAvatarUrl())
                                .ranking(ranking)
                                .point(leadScore)
                                .tier(null)
                                .totalSpending(spending)
                                .totalViewings(viewingsRequested)
                                .totalContracts(totalContracts)
                                .createdAt(customerUser.getCreatedAt())
                                .updatedAt(customerUser.getUpdatedAt())
                                .location(customerUser.getWard().getWardName() + ", " +
                                         customerUser.getWard().getDistrict().getDistrictName() + ", " +
                                         customerUser.getWard().getDistrict().getCity().getCityName())
                                .build()
                );
            }
        }

        customerListItemList = customerListItemList.stream()
                .sorted(Comparator.comparing(CustomerListItem::getPoint, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();


        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), customerListItemList.size());
        List<CustomerListItem> pageContent = customerListItemList.subList(start, end);

        return new PageImpl<>(pageContent, pageable, customerListItemList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyOwnerListItem> getAllPropertyOwnerItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<Constants.ContributionTierEnum> ownerTiers,
            Integer minContributionPoint, Integer maxContributionPoint,
            Integer minProperties, Integer maxProperties,
            Integer minPropertiesForSale, Integer maxPropertiesForSale,
            Integer minPropertiesForRents, Integer maxPropertiesForRents,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds) {

        List<PropertyOwnerListItem> ownerListItemList = new ArrayList<>();

        List<User> propertyOwners = userRepository.findAllByPropertyOwnerFullNameIsLikeIgnoreCaseAndRangeJoinedDateAndLocation(
                name,
                cityIds, districtIds, wardIds
        );

        boolean findByMonth = month != null;

        for (User ownerUser : propertyOwners) {
            // Filter by joinedDate (createdAt) in service layer
            LocalDateTime ownerJoinedDate = ownerUser.getCreatedAt();
            if (joinedDateFrom != null && ownerJoinedDate.isBefore(joinedDateFrom)) {
                continue;
            }
            if (joinedDateTo != null && ownerJoinedDate.isAfter(joinedDateTo)) {
                continue;
            }

            if (findByMonth) {
                IndividualPropertyOwnerContributionMonth ownerContributionMonth = rankingService.getPropertyOwnerMonth(
                        ownerUser.getId(),
                        month,
                        year
                );

                // Filter by owner tiers if provided
                if (ownerTiers != null && !ownerTiers.isEmpty()) {
                    boolean found = false;
                    for (var tier : ownerTiers) {
                        if (ownerContributionMonth.getContributionTier() == tier) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }

                Integer contributionPoint = ownerContributionMonth.getContributionPoint();
                if (minContributionPoint != null && contributionPoint < minContributionPoint) {
                    continue;
                }
                if (maxContributionPoint != null && contributionPoint > maxContributionPoint) {
                    continue;
                }

                Integer ranking = ownerContributionMonth.getRankingPosition();
                if (minRanking != null && ranking < minRanking) {
                    continue;
                }
                if (maxRanking != null && ranking > maxRanking) {
                    continue;
                }

                Integer totalProperties = ownerContributionMonth.getMonthTotalProperties();
                if (minProperties != null && totalProperties < minProperties) {
                    continue;
                }
                if (maxProperties != null && totalProperties > maxProperties) {
                    continue;
                }

                Integer propertiesForSale = ownerContributionMonth.getMonthTotalForSales();
                if (minPropertiesForSale != null && propertiesForSale < minPropertiesForSale) {
                    continue;
                }
                if (maxPropertiesForSale != null && propertiesForSale > maxPropertiesForSale) {
                    continue;
                }

                Integer propertiesForRents = ownerContributionMonth.getMonthTotalForRents();
                if (minPropertiesForRents != null && propertiesForRents < minPropertiesForRents) {
                    continue;
                }
                if (maxPropertiesForRents != null && propertiesForRents > maxPropertiesForRents) {
                    continue;
                }

                ownerListItemList.add(
                        PropertyOwnerListItem.builder()
                                .id(ownerUser.getId())
                                .firstName(ownerUser.getFirstName())
                                .lastName(ownerUser.getLastName())
                                .avatarUrl(ownerUser.getAvatarUrl())
                                .ranking(ranking)
                                .point(contributionPoint)
                                .tier(ownerContributionMonth.getContributionTier().getValue())
                                .totalValue(ownerContributionMonth.getMonthContributionValue())
                                .totalProperties(totalProperties)
                                .createdAt(ownerUser.getCreatedAt())
                                .updatedAt(ownerUser.getUpdatedAt())
                                .location(ownerUser.getWard().getWardName() + ", " +
                                         ownerUser.getWard().getDistrict().getDistrictName() + ", " +
                                         ownerUser.getWard().getDistrict().getCity().getCityName())
                                .build()
                );
            } else {
                IndividualPropertyOwnerContributionAll ownerContributionAll = rankingService.getPropertyOwnerAll(ownerUser.getId());

                Integer contributionPoint = ownerContributionAll.getContributionPoint();
                if (minContributionPoint != null && contributionPoint < minContributionPoint) {
                    continue;
                }
                if (maxContributionPoint != null && contributionPoint > maxContributionPoint) {
                    continue;
                }

                Integer ranking = ownerContributionAll.getRankingPosition();
                if (minRanking != null && ranking < minRanking) {
                    continue;
                }
                if (maxRanking != null && ranking > maxRanking) {
                    continue;
                }

                Integer totalProperties = ownerContributionAll.getTotalProperties();
                if (minProperties != null && totalProperties < minProperties) {
                    continue;
                }
                if (maxProperties != null && totalProperties > maxProperties) {
                    continue;
                }

                // All schema doesn't have separate for_sale/for_rent counts
                // Use sold/rented as approximation or skip these filters
                Integer propertiesSold = ownerContributionAll.getTotalPropertiesSold();
                Integer propertiesRented = ownerContributionAll.getTotalPropertiesRented();

                if (minPropertiesForSale != null && propertiesSold < minPropertiesForSale) {
                    continue;
                }
                if (maxPropertiesForSale != null && propertiesSold > maxPropertiesForSale) {
                    continue;
                }

                if (minPropertiesForRents != null && propertiesRented < minPropertiesForRents) {
                    continue;
                }
                if (maxPropertiesForRents != null && propertiesRented > maxPropertiesForRents) {
                    continue;
                }

                ownerListItemList.add(
                        PropertyOwnerListItem.builder()
                                .id(ownerUser.getId())
                                .firstName(ownerUser.getFirstName())
                                .lastName(ownerUser.getLastName())
                                .avatarUrl(ownerUser.getAvatarUrl())
                                .ranking(ranking)
                                .point(contributionPoint)
                                .tier(rankingService.getCurrentTier(ownerUser.getId(), Constants.RoleEnum.PROPERTY_OWNER))
                                .totalValue(ownerContributionAll.getContributionValue())
                                .totalProperties(totalProperties)
                                .createdAt(ownerUser.getCreatedAt())
                                .updatedAt(ownerUser.getUpdatedAt())
                                .location(ownerUser.getWard().getWardName() + ", " +
                                         ownerUser.getWard().getDistrict().getDistrictName() + ", " +
                                         ownerUser.getWard().getDistrict().getCity().getCityName())
                                .build()
                );
            }
        }

        // sort asc by point
        ownerListItemList = ownerListItemList.stream()
                .sorted(Comparator.comparing(PropertyOwnerListItem::getPoint, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ownerListItemList.size());
        List<PropertyOwnerListItem> pageContent = ownerListItemList.subList(start, end);

        return new PageImpl<>(pageContent, pageable, ownerListItemList.size());
    }

    @Override
    public Page<FreeAgentListItem> getAllFreeAgentItemsWithFilters(
            Pageable pageable,
            String agentNameOrCode,
            List<Constants.PerformanceTierEnum> agentTiers,
            Integer minAssignedAppointments, Integer maxAssignedAppointments,
            Integer minAssignedProperties, Integer maxAssignedProperties,
            Integer minCurrentlyHandle, Integer maxCurrentlyHandle
    ) {
        // Get all sale agents
        List<User> agents;
        if (agentNameOrCode != null && !agentNameOrCode.isEmpty()) {
            // Search by name or employee code
            agents = userRepository.findAllByFullNameIsLikeIgnoreCaseAndRole(agentNameOrCode, Constants.RoleEnum.SALESAGENT);
            // Also search by employee code
            List<User> agentsByCode = userRepository.findAllByRole(Constants.RoleEnum.SALESAGENT).stream()
                    .filter(u -> u.getSaleAgent() != null &&
                            u.getSaleAgent().getEmployeeCode() != null &&
                            u.getSaleAgent().getEmployeeCode().toLowerCase().contains(agentNameOrCode.toLowerCase()))
                    .toList();
            // Merge and remove duplicates
            List<User> combined = new ArrayList<>(agents);
            for (User agent : agentsByCode) {
                if (!combined.contains(agent)) {
                    combined.add(agent);
                }
            }
            agents = combined;
        } else {
            agents = userRepository.findAllByRole(Constants.RoleEnum.SALESAGENT);
        }

        List<FreeAgentListItem> freeAgentList = new ArrayList<>();

        for (User agentUser : agents) {
            SaleAgent saleAgent = agentUser.getSaleAgent();
            if (saleAgent == null) continue;

            // Count assigned appointments using service
            int assignedAppointments = appointmentService.countByAgentId(saleAgent.getId());

            // Count assigned properties using service
            int assignedProperties = propertyService.countByAssignedAgentId(saleAgent.getId());

            // Calculate currently handling
            int currentlyHandling = assignedAppointments + assignedProperties;

            // Filter by assigned appointments
            if (minAssignedAppointments != null && assignedAppointments < minAssignedAppointments) {
                continue;
            }
            if (maxAssignedAppointments != null && assignedAppointments > maxAssignedAppointments) {
                continue;
            }

            // Filter by assigned properties
            if (minAssignedProperties != null && assignedProperties < minAssignedProperties) {
                continue;
            }
            if (maxAssignedProperties != null && assignedProperties > maxAssignedProperties) {
                continue;
            }

            // Filter by currently handling
            if (minCurrentlyHandle != null && currentlyHandling < minCurrentlyHandle) {
                continue;
            }
            if (maxCurrentlyHandle != null && currentlyHandling > maxCurrentlyHandle) {
                continue;
            }

            // Get agent tier from ranking service
            IndividualSalesAgentPerformanceMonth agentPerformanceMonth = rankingService.getSaleAgentCurrentMonth(agentUser.getId());
            String tier = agentPerformanceMonth.getPerformanceTier().getValue();

            // Filter by tier
            if (agentTiers != null && !agentTiers.isEmpty()) {
                boolean tierMatched = false;
                for (Constants.PerformanceTierEnum desiredTier : agentTiers) {
                    if (agentPerformanceMonth.getPerformanceTier() == desiredTier) {
                        tierMatched = true;
                        break;
                    }
                }
                if (!tierMatched) {
                    continue;
                }
            }

            // Build FreeAgentListItem
            FreeAgentListItem freeAgentListItem = FreeAgentListItem.builder()
                    .id(agentUser.getId())
                    .createdAt(agentUser.getCreatedAt())
                    .updatedAt(agentUser.getUpdatedAt())
                    .fullName(agentUser.getFullName())
                    .ranking(agentPerformanceMonth.getRankingPosition())
                    .employeeCode(saleAgent.getEmployeeCode())
                    .avatarUrl(agentUser.getAvatarUrl())
                    .tier(tier)
                    .assignedAppointments(assignedAppointments)
                    .assignedProperties(assignedProperties)
                    .currentlyHandling(currentlyHandling)
                    .maxProperties(saleAgent.getMaxProperties())
                    .build();

            freeAgentList.add(freeAgentListItem);
        }

        // sort asc by point
        freeAgentList = freeAgentList.stream()
                .sorted(Comparator.comparing(FreeAgentListItem::getRanking, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), freeAgentList.size());
        List<FreeAgentListItem> pageContent = freeAgentList.subList(start, end);

        return new PageImpl<>(pageContent, pageable, freeAgentList.size());
    }

    @Override
    public List<User> findAllByNameAndRole(String name, Constants.RoleEnum roleEnum) {
        if (name != null && !name.isEmpty())
            return userRepository.findAllByFullNameIsLikeIgnoreCaseAndRole(name, roleEnum);
        else
            return userRepository.findAllByRole(roleEnum);
    }

    @Override
    public List<User> findAllByRoleAndStillAvailable(Constants.RoleEnum roleEnum) {
        return userRepository.findAllByRoleAndStatusIn(
                roleEnum,
                List.of(Constants.StatusProfileEnum.ACTIVE)
        );
    }

    @Override
    public Integer countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum roleEnum, int month, int year) {
        if (month == 0 || year == 0)
            return userRepository.countByRole(roleEnum);
        return userRepository.countByRoleAndCreatedAtYearAndMonth(roleEnum, year, month);
    }

    @Override
    public List<UUID> getAllCurrentAgentIds() {
        return userRepository.findAllCurrentAgent();
    }

}
