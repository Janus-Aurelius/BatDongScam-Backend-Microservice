package com.se.bds.common.message.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG13 {
    public static final String CODE = "MSG 13";
    public static final String MESSAGE = "Invalid range or value detected. Please ensure dates and amounts are correct.";
    public static final String TYPE = "Error";
}
