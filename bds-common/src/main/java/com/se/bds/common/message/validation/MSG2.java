package com.se.bds.common.message.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG2 {
    public static final String CODE = "MSG 2";
    public static final String MESSAGE = "One or more required fields are empty. Please check your input.";
    public static final String TYPE = "Error";
}
