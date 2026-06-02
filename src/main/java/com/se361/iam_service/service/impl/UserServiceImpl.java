package com.se361.iam_service.service.impl;

import com.se361.iam_service.dto.request.RegisterRequest;
import com.se361.iam_service.dto.request.UpdateAccountDto;
import com.se361.iam_service.dto.response.UserResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

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

        // tier, statisticMonth, statisticAll → null tạm thời
        // Sẽ được enrich bởi API Gateway hoặc BFF sau khi có Ranking Service

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
