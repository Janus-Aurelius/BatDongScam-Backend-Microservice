package com.se.bds.common.message.discovery;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG30 {
    public static final String CODE = "MSG 30";
    public static final String MESSAGE = "Invalid phone number format.";
    public static final String TYPE = "Error";
}
