package com.se100.bds.dtos.responses.user.otherprofile;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserProfileResponse<T> extends AbstractBaseDataResponse {
    private String role;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String zaloContact;
    private String status;
    private String tier;
    private UUID wardId;
    private String wardName;
    private UUID districtId;
    private String districtName;
    private UUID cityId;
    private String cityName;
    private String avatarUrl;
    private String identificationNumber;
    private LocalDate dayOfBirth;
    private String gender;
    private T propertyProfile;
}
