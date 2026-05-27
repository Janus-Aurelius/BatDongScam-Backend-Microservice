package com.se.bds.common.message.authentication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG26 {
    public static final String CODE = "MSG 26";
    public static final String MESSAGE = "This phone number is already registered.";
    public static final String TYPE = "Error";
}
