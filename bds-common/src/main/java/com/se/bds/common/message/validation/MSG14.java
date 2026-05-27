package com.se.bds.common.message.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG14 {
    public static final String CODE = "MSG 14";
    public static final String MESSAGE = "The description provided is too short. Please provide more detail.";
    public static final String TYPE = "Error";
}
