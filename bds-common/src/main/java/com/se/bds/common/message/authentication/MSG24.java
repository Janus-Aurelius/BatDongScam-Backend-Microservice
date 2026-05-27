package com.se.bds.common.message.authentication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG24 {
    public static final String CODE = "MSG 24";
    public static final String MESSAGE = "Password must be at least 8 characters long.";
    public static final String TYPE = "Error";
}
