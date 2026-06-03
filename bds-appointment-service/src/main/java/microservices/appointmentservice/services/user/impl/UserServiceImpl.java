package microservices.appointmentservice.services.user.impl;

import microservices.appointmentservice.dtos.requests.account.UpdateAccountDto;
import microservices.appointmentservice.dtos.requests.auth.RegisterRequest;
import microservices.appointmentservice.dtos.responses.user.listitem.CustomerListItem;
import microservices.appointmentservice.dtos.responses.user.listitem.FreeAgentListItem;
import microservices.appointmentservice.dtos.responses.user.listitem.PropertyOwnerListItem;
import microservices.appointmentservice.dtos.responses.user.listitem.SaleAgentListItem;
import microservices.appointmentservice.dtos.responses.user.meprofile.MeResponse;
import microservices.appointmentservice.dtos.responses.user.otherprofile.UserProfileResponse;
import microservices.appointmentservice.entities.user.SaleAgent;
import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.exceptions.NotFoundException;
import microservices.appointmentservice.repositories.UserRepository;
import microservices.appointmentservice.services.user.UserService;
import microservices.appointmentservice.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUser() {
        UUID userId = getUserId();
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public UUID getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof String userIdStr) {
            try {
                return UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in principal string: {}", userIdStr);
                return null;
            }
        }
        return null;
    }



    @Override
    public @NotNull User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found: " + id));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    @Override
    public List<User> findAllByNameAndRole(String name, Constants.RoleEnum roleEnum) {
        if (name == null && roleEnum == null) {
            return userRepository.findAll();
        }
        if (roleEnum == null) {
            return userRepository.findAllByNameAndRole(name, null);
        }
        return userRepository.findAllByNameAndRole(name, roleEnum);
    }

    @Override
    public List<User> findAllByRoleAndStillAvailable(Constants.RoleEnum roleEnum) {
        return userRepository.findAllByRole(roleEnum);
    }

    @Override
    public List<User> getAllByName(String name) {
        if (name == null || name.isBlank()) return userRepository.findAll();
        return userRepository.findAllByNameAndRole(name, null);
    }

    @Override
    public SaleAgent findSaleAgentById(UUID agentId) {
        User user = findById(agentId);
        if (user.getSaleAgent() == null) {
            throw new NotFoundException("Sale agent not found: " + agentId);
        }
        return user.getSaleAgent();
    }

    @Override
    public User updateStatus(UUID id, String status) {
        User user = findById(id);
        user.setStatus(Constants.StatusProfileEnum.get(status));
        return userRepository.save(user);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public void approveAccount(UUID propOwnerId, boolean approve) {
        User user = findById(propOwnerId);
        user.setStatus(approve ? Constants.StatusProfileEnum.ACTIVE : Constants.StatusProfileEnum.REJECTED);
        userRepository.save(user);
    }

    @Override
    public MeResponse<?> getAccount() {
        throw new UnsupportedOperationException("getAccount is not supported in appointment-service");
    }

    @Override
    public MeResponse<?> getUserById(UUID userId) {
        throw new UnsupportedOperationException("getUserById is not supported in appointment-service");
    }

    @Override
    public MeResponse<?> updateUserById(UUID userId, UpdateAccountDto updateAccountDto) {
        throw new UnsupportedOperationException("updateUserById is not supported in appointment-service");
    }

    @Override
    public MeResponse<?> updateMe(UpdateAccountDto updateAccountDto) {
        throw new UnsupportedOperationException("updateMe is not supported in appointment-service");
    }

    @Override
    public void deleteAccountById(UUID userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public void deleteMyAccount() {
        UUID userId = getUserId();
        if (userId != null) userRepository.deleteById(userId);
    }

    @Override
    public UserProfileResponse<?> getUserProfileById(UUID id) {
        throw new UnsupportedOperationException("getUserProfileById is not supported in appointment-service");
    }

    @Override
    public User register(RegisterRequest request, Constants.RoleEnum roleEnum) throws BindException, IOException {
        throw new UnsupportedOperationException("register is not supported in appointment-service");
    }

    @Override
    public void delete(String id) {
        userRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public void activeteUser(String id) {
        User user = findById(UUID.fromString(id));
        user.setStatus(Constants.StatusProfileEnum.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public Page<SaleAgentListItem> getAllSaleAgentItemsWithFilters(
            Pageable pageable, String name, Integer month, Integer year,
            List<Constants.PerformanceTierEnum> agentTiers, Integer maxProperties,
            Integer minPerformancePoint, Integer maxPerformancePoint,
            Integer minRanking, Integer maxRanking,
            Integer minAssignments, Integer maxAssignments,
            Integer minAssignedProperties, Integer maxAssignedProperties,
            Integer minAssignedAppointments, Integer maxAssignedAppointments,
            Integer minContracts, Integer maxContracts,
            Double minAvgRating, Double maxAvgRating,
            LocalDateTime hiredDateFrom, LocalDateTime hiredDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds) {
        return Page.empty(pageable);
    }

    @Override
    public Page<CustomerListItem> getAllCustomerItemsWithFilters(
            Pageable pageable, String name, Integer month, Integer year,
            List<Constants.CustomerTierEnum> customerTierEnums,
            Integer minLeadingScore, Integer maxLeadingScore,
            Integer minViewings, Integer maxViewings,
            BigDecimal minSpending, BigDecimal maxSpending,
            Integer minContracts, Integer maxContracts,
            Integer minPropertiesBought, Integer maxPropertiesBought,
            Integer minPropertiesRented, Integer maxPropertiesRented,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds) {
        return Page.empty(pageable);
    }

    @Override
    public Page<PropertyOwnerListItem> getAllPropertyOwnerItemsWithFilters(
            Pageable pageable, String name, Integer month, Integer year,
            List<Constants.ContributionTierEnum> ownerTiers,
            Integer minContributionPoint, Integer maxContributionPoint,
            Integer minProperties, Integer maxProperties,
            Integer minPropertiesForSale, Integer maxPropertiesForSale,
            Integer minPropertiesForRents, Integer maxPropertiesForRents,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds) {
        return Page.empty(pageable);
    }

    @Override
    public Page<FreeAgentListItem> getAllFreeAgentItemsWithFilters(
            Pageable pageable, String agentNameOrCode,
            List<Constants.PerformanceTierEnum> agentTiers,
            Integer minAssignedAppointments, Integer maxAssignedAppointments,
            Integer minAssignedProperties, Integer maxAssignedProperties,
            Integer minCurrentlyHandle, Integer maxCurrentlyHandle) {
        return Page.empty(pageable);
    }

    @Override
    public Integer countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum roleEnum, int month, int year) {
        return 0;
    }

    @Override
    public List<UUID> getAllCurrentAgentIds() {
        return Collections.emptyList();
    }
}
