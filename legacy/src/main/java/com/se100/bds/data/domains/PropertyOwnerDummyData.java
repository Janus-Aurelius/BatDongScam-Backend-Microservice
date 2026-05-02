package com.se100.bds.data.domains;

import com.se100.bds.data.util.TimeGenerator;
import com.se100.bds.models.entities.location.Ward;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.domains.location.WardRepository;
import com.se100.bds.repositories.domains.user.UserRepository;
import com.se100.bds.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class PropertyOwnerDummyData {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WardRepository wardRepository;
    private final TimeGenerator timeGenerator = new TimeGenerator();

    public void createDummy() {
        createDummyPropertyOwners();
    }

    private void createDummyPropertyOwners() {
        log.info("Creating dummy property owners");

        List<User> owners = new ArrayList<>();
        List<Ward> wards = wardRepository.findAll();
        Random random = new Random();

        // Create 20 Property Owners
        for (int i = 1; i <= 20; i++) {
            LocalDateTime createdAt = timeGenerator.getRandomTime();
            LocalDateTime updatedAt = timeGenerator.getRandomTimeAfter(createdAt, LocalDateTime.now());

            User user = createUser(
                    String.format("owner%d@example.com", i),
                    String.format("092%07d", i),
                    "Owner",
                    String.format("Number%d", i),
                    wards.isEmpty() ? null : wards.get(random.nextInt(wards.size())),
                    String.format("ID%09d", 100000000 + i),
                    createdAt,
                    updatedAt
            );

            PropertyOwner propertyOwner = createPropertyOwner(user, createdAt, updatedAt);
            user.setPropertyOwner(propertyOwner);
            owners.add(user);
        }

        userRepository.saveAll(owners);
        log.info("Saved {} property owners to database", owners.size());
    }

    private User createUser(String email, String phoneNumber, String firstName, String lastName, Ward ward,
                           String identificationNumber, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Random random = new Random();

        // Generate random date of birth (age between 25 and 70 years old for property owners)
        int yearsOld = 25 + random.nextInt(46);
        LocalDate dayOfBirth = LocalDate.now().minusYears(yearsOld).minusDays(random.nextInt(365));

        // Random gender
        String gender = random.nextBoolean() ? "Male" : "Female";

        // Issue date should be after 18 years old and before now
        LocalDate issueDate = dayOfBirth.plusYears(18).plusDays(random.nextInt(365 * (yearsOld - 18)));

        // Generate avatar URL based on gender
        String avatarUrl = String.format("https://avatar.iran.liara.run/public/%s?username=%s",
                gender.toLowerCase(), email.split("@")[0]);

        User user = User.builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .zaloContact(phoneNumber)
                .firstName(firstName)
                .lastName(lastName)
                .ward(ward)
                .password(passwordEncoder.encode("P@sswd123."))
                .role(Constants.RoleEnum.PROPERTY_OWNER)
                .status(Constants.StatusProfileEnum.ACTIVE)
                .avatarUrl(avatarUrl)
                .identificationNumber(identificationNumber)
                .dayOfBirth(dayOfBirth)
                .gender(gender)
                .nation("Vietnam")
                .issueDate(issueDate)
                .issuingAuthority("Public Security Department")
                .frontIdPicturePath(String.format("/uploads/id_cards/%s_front.jpg", identificationNumber))
                .backIdPicturePath(String.format("/uploads/id_cards/%s_back.jpg", identificationNumber))
                .lastLoginAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                .notifications(new ArrayList<>())
                .build();

        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        return user;
    }

    private PropertyOwner createPropertyOwner(User user, LocalDateTime createdAt, LocalDateTime updatedAt) {
        LocalDateTime approvedAt = timeGenerator.getRandomTimeAfter(updatedAt, LocalDateTime.now());

        PropertyOwner owner = PropertyOwner.builder()
                .user(user)
                .approvedAt(approvedAt)
                .properties(new ArrayList<>())
                .build();

        owner.setCreatedAt(createdAt);
        owner.setUpdatedAt(updatedAt);
        return owner;
    }
}
