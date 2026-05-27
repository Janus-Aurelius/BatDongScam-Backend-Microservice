package com.se.bds.common.message.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG12 {
    public static final String CODE = "MSG 12";
    public static final String MESSAGE = "This action is not allowed for the current item status or your user role.";
    public static final String TYPE = "Error";
}
