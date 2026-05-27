package com.se.bds.common.message.authentication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG27 {
    public static final String CODE = "MSG 27";
    public static final String MESSAGE = "This email address is already registered.";
    public static final String TYPE = "Error";
}
