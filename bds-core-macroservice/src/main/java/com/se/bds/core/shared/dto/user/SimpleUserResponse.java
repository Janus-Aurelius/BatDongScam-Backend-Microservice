package com.se.bds.core.shared.dto.user;

import com.se.bds.common.dto.AbstractBaseDataResponse;
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
