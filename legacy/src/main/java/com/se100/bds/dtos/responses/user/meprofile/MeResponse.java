package com.se100.bds.dtos.responses.user.meprofile;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class MeResponse<T> extends AbstractBaseDataResponse {
    private String role;
    private String email;
    private String tier;
    private String phoneNumber;
    private String zaloContact;
    private UUID wardId;
    private String wardName;
    private UUID districtId;
    private String districtName;
    private UUID cityId;
    private String cityName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String status;
    private String identificationNumber;
    private LocalDate dayOfBirth;
    private String gender;
    private String nation;
    private LocalDate issueDate;
    private String issuingAuthority;
    private String frontIdPicturePath;
    private String backIdPicturePath;
    private LocalDateTime lastLoginAt;
    private T profile;
    private T statisticMonth;
    private T statisticAll;
}