package com.se361.iam_service.entity;

import com.se361.iam_service.util.Constants;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "user_id", nullable = false))
})
public class User extends AbstractBaseEntity{
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Constants.RoleEnum role;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "zalo_contact")
    private String zaloContact;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Constants.StatusProfileEnum status;

    @Column(name = "identification_number")
    private String identificationNumber;

    @Column(name = "day_of_birth")
    private LocalDate dayOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "nation")
    private String nation;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_account_name", length = 150)
    private String bankAccountName;

    @Column(name = "bank_bin", length = 20)
    private String bankBin;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "issuing_authority")
    private String issuingAuthority;

    @Column(name = "front_id_picture_path")
    private String frontIdPicturePath;

    @Column(name = "back_id_picture_path")
    private String backIdPicturePath;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "fcm_token")
    private String fcmToken;

    // Ward chưa tách sang Location Service → giữ ward_id dạng UUID thuần
    @Column(name = "ward_id")
    private UUID wardId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Customer customer;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SaleAgent saleAgent;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private PropertyOwner propertyOwner;

    public String getFullName() {
        return lastName + " " + firstName;
    }
}
