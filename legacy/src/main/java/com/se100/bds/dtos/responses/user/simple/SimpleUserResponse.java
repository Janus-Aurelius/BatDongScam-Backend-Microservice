package com.se100.bds.dtos.responses.user.simple;

import com.se100.bds.dtos.responses.AbstractBaseDataResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SimpleUserResponse extends AbstractBaseDataResponse {
    private String firstName;
    private String lastName;
    private String tier;
    private String zaloContact;
    private String phoneNumber;
}

