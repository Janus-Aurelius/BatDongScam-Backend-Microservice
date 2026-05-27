package com.se.bds.common.message.authentication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG25 {
    public static final String CODE = "MSG 25";
    public static final String MESSAGE = "Password does not meet complexity requirements.";
    public static final String TYPE = "Error";
}
