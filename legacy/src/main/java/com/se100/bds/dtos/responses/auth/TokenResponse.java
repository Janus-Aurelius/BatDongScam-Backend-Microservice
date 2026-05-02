package com.se100.bds.dtos.responses.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class TokenResponse {
    private String token;
    private String refreshToken;
    private String userId;
    private String role;
}
