package com.se361.iam_service.service.impl;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.enums.ContributionTierEnum;
import com.se.bds.common.enums.CustomerTierEnum;
import com.se.bds.common.enums.PerformanceTierEnum;
import com.se.bds.common.enums.RoleEnum;
import com.se361.iam_service.client.LocationClient;
import com.se361.iam_service.client.RankingClient;
import com.se361.iam_service.dto.request.RegisterRequest;
import com.se361.iam_service.dto.request.UpdateAccountDto;
import com.se361.iam_service.dto.response.UserResponse;
import com.se361.iam_service.dto.response.listitem.CustomerListItem;
import com.se361.iam_service.dto.response.listitem.PropertyOwnerListItem;
import com.se361.iam_service.dto.response.listitem.SaleAgentListItem;
import com.se361.iam_service.dto.response.ranking.*;
import com.se361.iam_service.entity.Customer;
import com.se361.iam_service.entity.PropertyOwner;
import com.se361.iam_service.entity.SaleAgent;
import com.se361.iam_service.entity.User;
import com.se361.iam_service.repository.CustomerRepository;
import com.se361.iam_service.repository.PropertyOwnerRepository;
import com.se361.iam_service.repository.SaleAgentRepository;
import com.se361.iam_service.repository.UserRepository;
import com.se361.iam_service.security.JwtUserDetails;
import com.se361.iam_service.service.CloudinaryService;
import com.se361.iam_service.service.UserService;
import com.se361.iam_service.util.Constants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final RankingClient rankingClient;
    private final LocationClient locationClient;

    private final ConcurrentHashMap<UUID, String> locationCache = new ConcurrentHashMap<>();

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()){
            throw new BadCredentialsException("Not authenticated");
        }
        JwtUserDetails principal = (JwtUserDetails) auth.getPrincipal();
        return findById(UUID.fromString(principal.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    @Override
    public UserResponse getMe() {
        return toResponse(getCurrentUser());
    }

    @Override
    public UserResponse getUserById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public UserResponse updateMe(UpdateAccountDto dto) {
        return updateUserById(getCurrentUser().getId(), dto);
    }

    @Override
    @Transactional
    public UserResponse updateUserById(UUID id, UpdateAccountDto dto) {
        User user = findById(id);
        applyUpdates(user, dto);
        userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteMyAccount() {
        deleteAccountById(getCurrentUser().getId());
    }

    @Override
    @Transactional
    public void deleteAccountById(UUID id) {
        User user = findById(id);
        if (user.getRole() == Constants.RoleEnum.ADMIN) {
            userRepository.delete(user);
        } else {
            user.setStatus(Constants.StatusProfileEnum.DELETED);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String frontIdUrl = null;
        String backIdUrl  = null;
        try {
            if (request.getFrontIdPicture() != null && !request.getFrontIdPicture().isEmpty()) {
                frontIdUrl = cloudinaryService.uploadFile(request.getFrontIdPicture(), "id_pictures");
            }
            if (request.getBackIdPicture() != null && !request.getBackIdPicture().isEmpty()) {
                backIdUrl = cloudinaryService.uploadFile(request.getBackIdPicture(), "id_pictures");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload ID pictures", e);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .wardId(request.getWardId())
                .identificationNumber(request.getIdentificationNumber())
                .dayOfBirth(request.getDayOfBirth())
                .gender(request.getGender())
                .nation(request.getNation())
                .issueDate(request.getIssuedDate())
                .issuingAuthority(request.getIssuingAuthority())
                .role(request.getRole())
                .build();

        if (request.getRole() == Constants.RoleEnum.CUSTOMER){
            user.setStatus(Constants.StatusProfileEnum.ACTIVE);
            Customer customer = new Customer();
            customer.setUser(user);
            user.setCustomer(customer);
        } else {
            user.setStatus(Constants.StatusProfileEnum.PENDING_APPROVAL);
            PropertyOwner owner = new PropertyOwner();
            owner.setUser(user);
            user.setPropertyOwner(owner);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void approveAccount(UUID propOwnerId, boolean approve) {
        User user = findById(propOwnerId);
        if (user.getStatus() != Constants.StatusProfileEnum.PENDING_APPROVAL) return;

        if (approve) {
            user.setStatus(Constants.StatusProfileEnum.ACTIVE);
            propertyOwnerRepository.findById(propOwnerId).ifPresent(po -> {
                po.setApprovedAt(LocalDateTime.now());
                propertyOwnerRepository.save(po);
            });
        } else {
            user.setStatus(Constants.StatusProfileEnum.REJECTED);
            propertyOwnerRepository.findById(propOwnerId).ifPresent(po -> {
                po.setApprovedAt(null);
                propertyOwnerRepository.save(po);
            });
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateStatus(UUID id, String status) {
        User user = findById(id);
        user.setStatus(Constants.StatusProfileEnum.valueOf(status.toUpperCase()));
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllByRole(String role, String name, Pageable pageable) {
        Constants.RoleEnum roleEnum = Constants.RoleEnum.valueOf(role.toUpperCase());

        Page<User> users;
        if (name != null && !name.isBlank()) {
            users = userRepository.findAllByRoleAndFullNameContaining(roleEnum, name, pageable);
        } else {
            users = userRepository.findAllByRole(roleEnum, pageable);
        }

        return users.map(this::toResponse);
    }

    @Override
    public List<UUID> getAllCurrentAgentIds() {
        return userRepository.findAllCurrentAgentIds();
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        User user = findById(id);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ── Advanced Filtering ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<SaleAgentListItem> getAllSaleAgentItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<PerformanceTierEnum> agentTiers, Integer maxProperties,
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
        List<UUID> resolvedWardIds = resolveWardIds(cityIds, districtIds, wardIds);
        List<User> agents = userRepository.findAllSaleAgentWithFilters(name, maxProperties, resolvedWardIds);

        List<SaleAgentListItem> agentListItemList = new ArrayList<>();
        boolean findByMonth = (month != null);

        for (User agentUser : agents) {
            if (agentUser.getSaleAgent() == null) continue;
            LocalDateTime agentHiredDate = agentUser.getSaleAgent().getHiredDate();
            if (hiredDateFrom != null && agentHiredDate != null && agentHiredDate.isBefore(hiredDateFrom)) continue;
            if (hiredDateTo != null && agentHiredDate != null && agentHiredDate.isAfter(hiredDateTo)) continue;

            if (findByMonth) {
                try {
                    ApiResponse<IndividualSalesAgentPerformanceMonth> apiRes = rankingClient.getSaleAgentMonth(agentUser.getId(), month, year);
                    if (apiRes == null || !apiRes.isSuccess() || apiRes.getData() == null) continue;
                    IndividualSalesAgentPerformanceMonth performance = apiRes.getData();

                    if (agentTiers != null && !agentTiers.isEmpty() && !agentTiers.contains(performance.getPerformanceTier())) continue;

                    Integer points = performance.getPerformancePoint();
                    if (minPerformancePoint != null && points < minPerformancePoint) continue;
                    if (maxPerformancePoint != null && points > maxPerformancePoint) continue;

                    Integer ranking = performance.getRankingPosition();
                    if (minRanking != null && ranking < minRanking) continue;
                    if (maxRanking != null && ranking > maxRanking) continue;

                    Integer assignedProperties = performance.getMonthPropertiesAssigned();
                    if (minAssignedProperties != null && assignedProperties < minAssignedProperties) continue;
                    if (maxAssignedProperties != null && assignedProperties > maxAssignedProperties) continue;

                    Integer assignedAppointments = performance.getMonthAppointmentsAssigned();
                    if (minAssignedAppointments != null && assignedAppointments < minAssignedAppointments) continue;
                    if (maxAssignedAppointments != null && assignedAppointments > maxAssignedAppointments) continue;

                    int assignments = (assignedProperties != null ? assignedProperties : 0) + (assignedAppointments != null ? assignedAppointments : 0);
                    if (minAssignments != null && assignments < minAssignments) continue;
                    if (maxAssignments != null && assignments > maxAssignments) continue;

                    Integer monthContracts = performance.getMonthContracts();
                    if (minContracts != null && monthContracts < minContracts) continue;
                    if (maxContracts != null && monthContracts > maxContracts) continue;

                    BigDecimal avgRating = performance.getAvgRating();
                    if (minAvgRating != null && avgRating != null && avgRating.compareTo(BigDecimal.valueOf(minAvgRating)) < 0) continue;
                    if (maxAvgRating != null && avgRating != null && avgRating.compareTo(BigDecimal.valueOf(maxAvgRating)) > 0) continue;

                    agentListItemList.add(SaleAgentListItem.builder()
                            .id(agentUser.getId())
                            .createdAt(agentUser.getCreatedAt())
                            .updatedAt(agentUser.getUpdatedAt())
                            .firstName(agentUser.getFirstName())
                            .lastName(agentUser.getLastName())
                            .avatarUrl(agentUser.getAvatarUrl())
                            .ranking(ranking)
                            .employeeCode(agentUser.getSaleAgent().getEmployeeCode())
                            .point(points)
                            .tier(performance.getPerformanceTier() != null ? performance.getPerformanceTier().getValue() : null)
                            .totalAssignments(assignments)
                            .propertiesAssigned(assignedProperties)
                            .appointmentsAssigned(assignedAppointments)
                            .totalContracts(monthContracts)
                            .rating(avgRating != null ? avgRating.doubleValue() : 0.0)
                            .totalRates(performance.getMonthRates())
                            .hiredDate(agentHiredDate)
                            .location(resolveLocationName(agentUser.getWardId()))
                            .build());
                } catch (Exception e) {
                    log.error("Error fetching performance details for agent {}", agentUser.getId(), e);
                }
            } else {
                try {
                    ApiResponse<IndividualSalesAgentPerformanceCareer> apiRes = rankingClient.getSaleAgentCareer(agentUser.getId());
                    if (apiRes == null || !apiRes.isSuccess() || apiRes.getData() == null) continue;
                    IndividualSalesAgentPerformanceCareer career = apiRes.getData();

                    Integer points = career.getPerformancePoint();
                    if (minPerformancePoint != null && points < minPerformancePoint) continue;
                    if (maxPerformancePoint != null && points > maxPerformancePoint) continue;

                    Integer ranking = career.getCareerRanking();
                    if (minRanking != null && ranking < minRanking) continue;
                    if (maxRanking != null && ranking > maxRanking) continue;

                    Integer assignedProperties = career.getPropertiesAssigned();
                    if (minAssignedProperties != null && assignedProperties < minAssignedProperties) continue;
                    if (maxAssignedProperties != null && assignedProperties > maxAssignedProperties) continue;

                    Integer assignedAppointments = career.getAppointmentAssigned();
                    if (minAssignedAppointments != null && assignedAppointments < minAssignedAppointments) continue;
                    if (maxAssignedAppointments != null && assignedAppointments > maxAssignedAppointments) continue;

                    int assignments = (assignedProperties != null ? assignedProperties : 0) + (assignedAppointments != null ? assignedAppointments : 0);
                    if (minAssignments != null && assignments < minAssignments) continue;
                    if (maxAssignments != null && assignments > maxAssignments) continue;

                    Integer totalContracts = career.getTotalContracts();
                    if (minContracts != null && totalContracts < minContracts) continue;
                    if (maxContracts != null && totalContracts > maxContracts) continue;

                    BigDecimal avgRating = career.getAvgRating();
                    if (minAvgRating != null && avgRating != null && avgRating.compareTo(BigDecimal.valueOf(minAvgRating)) < 0) continue;
                    if (maxAvgRating != null && avgRating != null && avgRating.compareTo(BigDecimal.valueOf(maxAvgRating)) > 0) continue;

                    agentListItemList.add(SaleAgentListItem.builder()
                            .id(agentUser.getId())
                            .createdAt(agentUser.getCreatedAt())
                            .updatedAt(agentUser.getUpdatedAt())
                            .firstName(agentUser.getFirstName())
                            .lastName(agentUser.getLastName())
                            .avatarUrl(agentUser.getAvatarUrl())
                            .ranking(ranking)
                            .employeeCode(agentUser.getSaleAgent().getEmployeeCode())
                            .point(points)
                            .tier(null)
                            .totalAssignments(assignments)
                            .propertiesAssigned(assignedProperties)
                            .appointmentsAssigned(assignedAppointments)
                            .totalContracts(totalContracts)
                            .rating(avgRating != null ? avgRating.doubleValue() : 0.0)
                            .totalRates(career.getTotalRates())
                            .hiredDate(agentHiredDate)
                            .location(resolveLocationName(agentUser.getWardId()))
                            .build());
                } catch (Exception e) {
                    log.error("Error fetching career details for agent {}", agentUser.getId(), e);
                }
            }
        }

        agentListItemList.sort(Comparator.comparing(
                SaleAgentListItem::getPoint,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), agentListItemList.size());
        List<SaleAgentListItem> pageContent = (start < agentListItemList.size()) ? agentListItemList.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, agentListItemList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerListItem> getAllCustomerItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<CustomerTierEnum> customerTiers,
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
        List<UUID> resolvedWardIds = resolveWardIds(cityIds, districtIds, wardIds);
        List<User> customers = userRepository.findAllCustomerWithFilters(name, resolvedWardIds);

        List<CustomerListItem> customerListItemList = new ArrayList<>();
        boolean findByMonth = (month != null);

        for (User customerUser : customers) {
            LocalDateTime customerJoinedDate = customerUser.getCreatedAt();
            if (joinedDateFrom != null && customerJoinedDate != null && customerJoinedDate.isBefore(joinedDateFrom)) continue;
            if (joinedDateTo != null && customerJoinedDate != null && customerJoinedDate.isAfter(joinedDateTo)) continue;

            if (findByMonth) {
                try {
                    ApiResponse<IndividualCustomerPotentialMonth> apiRes = rankingClient.getCustomerMonth(customerUser.getId(), month, year);
                    if (apiRes == null || !apiRes.isSuccess() || apiRes.getData() == null) continue;
                    IndividualCustomerPotentialMonth potential = apiRes.getData();

                    if (customerTiers != null && !customerTiers.isEmpty() && !customerTiers.contains(potential.getCustomerTier())) continue;

                    Integer leadScore = potential.getLeadScore();
                    if (minLeadingScore != null && leadScore < minLeadingScore) continue;
                    if (maxLeadingScore != null && leadScore > maxLeadingScore) continue;

                    Integer ranking = potential.getLeadPosition();
                    if (minRanking != null && ranking < minRanking) continue;
                    if (maxRanking != null && ranking > maxRanking) continue;

                    Integer viewings = potential.getMonthViewingsRequested();
                    if (minViewings != null && viewings < minViewings) continue;
                    if (maxViewings != null && viewings > maxViewings) continue;

                    BigDecimal spending = potential.getMonthSpending();
                    if (minSpending != null && spending != null && spending.compareTo(minSpending) < 0) continue;
                    if (maxSpending != null && spending != null && spending.compareTo(maxSpending) > 0) continue;

                    Integer monthContracts = potential.getMonthContractsSigned();
                    if (minContracts != null && monthContracts < minContracts) continue;
                    if (maxContracts != null && monthContracts > maxContracts) continue;

                    Integer purchases = potential.getMonthPurchases();
                    if (minPropertiesBought != null && purchases < minPropertiesBought) continue;
                    if (maxPropertiesBought != null && purchases > maxPropertiesBought) continue;

                    Integer rentals = potential.getMonthRentals();
                    if (minPropertiesRented != null && rentals < minPropertiesRented) continue;
                    if (maxPropertiesRented != null && rentals > maxPropertiesRented) continue;

                    customerListItemList.add(CustomerListItem.builder()
                            .id(customerUser.getId())
                            .createdAt(customerUser.getCreatedAt())
                            .updatedAt(customerUser.getUpdatedAt())
                            .firstName(customerUser.getFirstName())
                            .lastName(customerUser.getLastName())
                            .avatarUrl(customerUser.getAvatarUrl())
                            .ranking(ranking)
                            .point(leadScore)
                            .tier(potential.getCustomerTier() != null ? potential.getCustomerTier().getValue() : null)
                            .totalSpending(spending)
                            .totalViewings(viewings)
                            .totalContracts(monthContracts)
                            .location(resolveLocationName(customerUser.getWardId()))
                            .build());
                } catch (Exception e) {
                    log.error("Error fetching performance details for customer {}", customerUser.getId(), e);
                }
            } else {
                try {
                    ApiResponse<IndividualCustomerPotentialAll> apiRes = rankingClient.getCustomerAll(customerUser.getId());
                    if (apiRes == null || !apiRes.isSuccess() || apiRes.getData() == null) continue;
                    IndividualCustomerPotentialAll potentialAll = apiRes.getData();

                    Integer leadScore = potentialAll.getLeadScore();
                    if (minLeadingScore != null && leadScore < minLeadingScore) continue;
                    if (maxLeadingScore != null && leadScore > maxLeadingScore) continue;

                    Integer ranking = potentialAll.getLeadPosition();
                    if (minRanking != null && ranking < minRanking) continue;
                    if (maxRanking != null && ranking > maxRanking) continue;

                    Integer viewings = potentialAll.getViewingsRequested();
                    if (minViewings != null && viewings < minViewings) continue;
                    if (maxViewings != null && viewings > maxViewings) continue;

                    BigDecimal spending = potentialAll.getSpending();
                    if (minSpending != null && spending != null && spending.compareTo(minSpending) < 0) continue;
                    if (maxSpending != null && spending != null && spending.compareTo(maxSpending) > 0) continue;

                    Integer totalContracts = potentialAll.getTotalContractsSigned();
                    if (minContracts != null && totalContracts < minContracts) continue;
                    if (maxContracts != null && totalContracts > maxContracts) continue;

                    Integer purchases = potentialAll.getTotalPurchases();
                    if (minPropertiesBought != null && purchases < minPropertiesBought) continue;
                    if (maxPropertiesBought != null && purchases > maxPropertiesBought) continue;

                    Integer rentals = potentialAll.getTotalRentals();
                    if (minPropertiesRented != null && rentals < minPropertiesRented) continue;
                    if (maxPropertiesRented != null && rentals > maxPropertiesRented) continue;

                    customerListItemList.add(CustomerListItem.builder()
                            .id(customerUser.getId())
                            .createdAt(customerUser.getCreatedAt())
                            .updatedAt(customerUser.getUpdatedAt())
                            .firstName(customerUser.getFirstName())
                            .lastName(customerUser.getLastName())
                            .avatarUrl(customerUser.getAvatarUrl())
                            .ranking(ranking)
                            .point(leadScore)
                            .tier(null)
                            .totalSpending(spending)
                            .totalViewings(viewings)
                            .totalContracts(totalContracts)
                            .location(resolveLocationName(customerUser.getWardId()))
                            .build());
                } catch (Exception e) {
                    log.error("Error fetching career details for customer {}", customerUser.getId(), e);
                }
            }
        }

        customerListItemList.sort(Comparator.comparing(
                CustomerListItem::getPoint,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), customerListItemList.size());
        List<CustomerListItem> pageContent = (start < customerListItemList.size()) ? customerListItemList.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, customerListItemList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyOwnerListItem> getAllPropertyOwnerItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<ContributionTierEnum> ownerTiers,
            Integer minContributionPoint, Integer maxContributionPoint,
            Integer minProperties, Integer maxProperties,
            Integer minPropertiesForSale, Integer maxPropertiesForSale,
            Integer minPropertiesForRents, Integer maxPropertiesForRents,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    ) {
        List<UUID> resolvedWardIds = resolveWardIds(cityIds, districtIds, wardIds);
        List<User> owners = userRepository.findAllPropertyOwnerWithFilters(name, resolvedWardIds);

        List<PropertyOwnerListItem> ownerListItemList = new ArrayList<>();
        boolean findByMonth = (month != null);

        for (User ownerUser : owners) {
            LocalDateTime ownerJoinedDate = ownerUser.getCreatedAt();
            if (joinedDateFrom != null && ownerJoinedDate != null && ownerJoinedDate.isBefore(joinedDateFrom)) continue;
            if (joinedDateTo != null && ownerJoinedDate != null && ownerJoinedDate.isAfter(joinedDateTo)) continue;

            if (findByMonth) {
                try {
                    ApiResponse<IndividualPropertyOwnerContributionMonth> apiRes = rankingClient.getPropertyOwnerMonth(ownerUser.getId(), month, year);
                    if (apiRes == null || !apiRes.isSuccess() || apiRes.getData() == null) continue;
                    IndividualPropertyOwnerContributionMonth contribution = apiRes.getData();

                    if (ownerTiers != null && !ownerTiers.isEmpty() && !ownerTiers.contains(contribution.getContributionTier())) continue;

                    Integer points = contribution.getContributionPoint();
                    if (minContributionPoint != null && points < minContributionPoint) continue;
                    if (maxContributionPoint != null && points > maxContributionPoint) continue;

                    Integer ranking = contribution.getRankingPosition();
                    if (minRanking != null && ranking < minRanking) continue;
                    if (maxRanking != null && ranking > maxRanking) continue;

                    Integer totalProperties = contribution.getMonthTotalProperties();
                    if (minProperties != null && totalProperties < minProperties) continue;
                    if (maxProperties != null && totalProperties > maxProperties) continue;

                    Integer propertiesForSale = contribution.getMonthTotalForSales();
                    if (minPropertiesForSale != null && propertiesForSale < minPropertiesForSale) continue;
                    if (maxPropertiesForSale != null && propertiesForSale > maxPropertiesForSale) continue;

                    Integer propertiesForRents = contribution.getMonthTotalForRents();
                    if (minPropertiesForRents != null && propertiesForRents < minPropertiesForRents) continue;
                    if (maxPropertiesForRents != null && propertiesForRents > maxPropertiesForRents) continue;

                    ownerListItemList.add(PropertyOwnerListItem.builder()
                            .id(ownerUser.getId())
                            .createdAt(ownerUser.getCreatedAt())
                            .updatedAt(ownerUser.getUpdatedAt())
                            .firstName(ownerUser.getFirstName())
                            .lastName(ownerUser.getLastName())
                            .avatarUrl(ownerUser.getAvatarUrl())
                            .ranking(ranking)
                            .point(points)
                            .tier(contribution.getContributionTier() != null ? contribution.getContributionTier().getValue() : null)
                            .totalValue(contribution.getMonthContributionValue())
                            .totalProperties(totalProperties)
                            .location(resolveLocationName(ownerUser.getWardId()))
                            .build());
                } catch (Exception e) {
                    log.error("Error fetching performance details for owner {}", ownerUser.getId(), e);
                }
            } else {
                try {
                    ApiResponse<IndividualPropertyOwnerContributionAll> apiRes = rankingClient.getPropertyOwnerAll(ownerUser.getId());
                    if (apiRes == null || !apiRes.isSuccess() || apiRes.getData() == null) continue;
                    IndividualPropertyOwnerContributionAll contributionAll = apiRes.getData();

                    Integer points = contributionAll.getContributionPoint();
                    if (minContributionPoint != null && points < minContributionPoint) continue;
                    if (maxContributionPoint != null && points > maxContributionPoint) continue;

                    Integer ranking = contributionAll.getRankingPosition();
                    if (minRanking != null && ranking < minRanking) continue;
                    if (maxRanking != null && ranking > maxRanking) continue;

                    Integer totalProperties = contributionAll.getTotalProperties();
                    if (minProperties != null && totalProperties < minProperties) continue;
                    if (maxProperties != null && totalProperties > maxProperties) continue;

                    ownerListItemList.add(PropertyOwnerListItem.builder()
                            .id(ownerUser.getId())
                            .createdAt(ownerUser.getCreatedAt())
                            .updatedAt(ownerUser.getUpdatedAt())
                            .firstName(ownerUser.getFirstName())
                            .lastName(ownerUser.getLastName())
                            .avatarUrl(ownerUser.getAvatarUrl())
                            .ranking(ranking)
                            .point(points)
                            .tier(null)
                            .totalValue(contributionAll.getContributionValue())
                            .totalProperties(totalProperties)
                            .location(resolveLocationName(ownerUser.getWardId()))
                            .build());
                } catch (Exception e) {
                    log.error("Error fetching career details for owner {}", ownerUser.getId(), e);
                }
            }
        }

        ownerListItemList.sort(Comparator.comparing(
                PropertyOwnerListItem::getPoint,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ownerListItemList.size());
        List<PropertyOwnerListItem> pageContent = (start < ownerListItemList.size()) ? ownerListItemList.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, ownerListItemList.size());
    }

    private List<UUID> resolveWardIds(List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds) {
        List<UUID> resolved = new ArrayList<>();
        if (wardIds != null && !wardIds.isEmpty()) {
            resolved.addAll(wardIds);
        }
        if (districtIds != null && !districtIds.isEmpty()) {
            for (UUID distId : districtIds) {
                try {
                    List<LocationClient.WardDto> wards = locationClient.getWardsByDistrictId(distId);
                    if (wards != null) {
                        for (var w : wards) {
                            resolved.add(w.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch wards for district {}: {}", distId, e.getMessage());
                }
            }
        }
        if (cityIds != null && !cityIds.isEmpty()) {
            for (UUID cityId : cityIds) {
                try {
                    List<LocationClient.DistrictDto> districts = locationClient.getDistrictsByCityId(cityId);
                    if (districts != null) {
                        for (var d : districts) {
                            List<LocationClient.WardDto> wards = locationClient.getWardsByDistrictId(d.getId());
                            if (wards != null) {
                                for (var w : wards) {
                                    resolved.add(w.getId());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch districts/wards for city {}: {}", cityId, e.getMessage());
                }
            }
        }
        return resolved.isEmpty() ? null : resolved;
    }

    private String resolveLocationName(UUID wardId) {
        if (wardId == null) return null;
        return locationCache.computeIfAbsent(wardId, id -> {
            try {
                List<LocationClient.CityDto> cities = locationClient.getAllCities();
                if (cities != null) {
                    for (var city : cities) {
                        List<LocationClient.DistrictDto> districts = locationClient.getDistrictsByCityId(city.getId());
                        if (districts != null) {
                            for (var district : districts) {
                                List<LocationClient.WardDto> wards = locationClient.getWardsByDistrictId(district.getId());
                                if (wards != null) {
                                    for (var ward : wards) {
                                        if (ward.getId().equals(id)) {
                                            return ward.getWardName() + ", " + district.getDistrictName() + ", " + city.getCityName();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to resolve location name for ward {}: {}", id, e.getMessage());
            }
            return "Unknown Location";
        });
    }

    // ── Mapping (manual, no ModelMapper) ──────────────────────────────────────

    private UserResponse toResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .gender(user.getGender())
                .nation(user.getNation())
                .dayOfBirth(user.getDayOfBirth())
                .identificationNumber(user.getIdentificationNumber())
                .issueDate(user.getIssueDate())
                .issuingAuthority(user.getIssuingAuthority())
                .zaloContact(user.getZaloContact())
                .bankAccountNumber(user.getBankAccountNumber())
                .bankAccountName(user.getBankAccountName())
                .bankBin(user.getBankBin())
                .wardId(user.getWardId())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (user.getSaleAgent() != null) {
            SaleAgent sa = user.getSaleAgent();
            builder.saleAgentProfile(UserResponse.SaleAgentProfile.builder()
                    .employeeCode(sa.getEmployeeCode())
                    .maxProperties(sa.getMaxProperties())
                    .hiredDate(sa.getHiredDate())
                    .build());
        }

        if (user.getPropertyOwner() != null) {
            builder.propertyOwnerProfile(UserResponse.PropertyOwnerProfile.builder()
                    .approvedAt(user.getPropertyOwner().getApprovedAt())
                    .build());
        }

        // Fetch user tier dynamically from Ranking client
        try {
            if (user.getRole() == Constants.RoleEnum.SALESAGENT ||
                    user.getRole() == Constants.RoleEnum.CUSTOMER ||
                    user.getRole() == Constants.RoleEnum.PROPERTY_OWNER) {
                RoleEnum commonRole = RoleEnum.valueOf(user.getRole().name());
                ApiResponse<String> tierResponse = rankingClient.getCurrentTier(user.getId(), commonRole);
                if (tierResponse != null && tierResponse.isSuccess()) {
                    builder.tier(tierResponse.getData());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch tier for user {}: {}", user.getId(), e.getMessage());
            builder.tier("BRONZE");
        }

        return builder.build();
    }

    private void applyUpdates(User user, UpdateAccountDto dto) {
        if (dto.getFirstName() != null)           user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null)            user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null)         user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getEmail() != null)               user.setEmail(dto.getEmail());
        if (dto.getDayOfBirth() != null)          user.setDayOfBirth(dto.getDayOfBirth());
        if (dto.getGender() != null)              user.setGender(dto.getGender());
        if (dto.getNation() != null)              user.setNation(dto.getNation());
        if (dto.getIdentificationNumber() != null) user.setIdentificationNumber(dto.getIdentificationNumber());
        if (dto.getIssuedDate() != null)          user.setIssueDate(dto.getIssuedDate());
        if (dto.getIssuingAuthority() != null)    user.setIssuingAuthority(dto.getIssuingAuthority());
        if (dto.getZaloContract() != null)        user.setZaloContact(dto.getZaloContract());
        if (dto.getWardId() != null)              user.setWardId(dto.getWardId());

        try {
            if (dto.getAvatar() != null && !dto.getAvatar().isEmpty()) {
                if (user.getAvatarUrl() != null) cloudinaryService.deleteFile(user.getAvatarUrl());
                user.setAvatarUrl(cloudinaryService.uploadFile(dto.getAvatar(), "avatars"));
            }
            if (dto.getFrontIdPicture() != null && !dto.getFrontIdPicture().isEmpty()) {
                if (user.getFrontIdPicturePath() != null) cloudinaryService.deleteFile(user.getFrontIdPicturePath());
                user.setFrontIdPicturePath(cloudinaryService.uploadFile(dto.getFrontIdPicture(), "id_pictures"));
            }
            if (dto.getBackIdPicture() != null && !dto.getBackIdPicture().isEmpty()) {
                if (user.getBackIdPicturePath() != null) cloudinaryService.deleteFile(user.getBackIdPicturePath());
                user.setBackIdPicturePath(cloudinaryService.uploadFile(dto.getBackIdPicture(), "id_pictures"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }
}
