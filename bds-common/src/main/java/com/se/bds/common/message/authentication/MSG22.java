package com.se.bds.common.message.authentication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG22 {
    public static final String CODE = "MSG 22";
    public static final String MESSAGE = "Invalid credentials or session expired. Please sign in again.";
    public static final String TYPE = "Error";
}
