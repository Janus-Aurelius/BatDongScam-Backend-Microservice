package com.se.bds.common.message.discovery;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG33 {
    public static final String CODE = "MSG 33";
    public static final String MESSAGE = "A pending or confirmed appointment already exists for this property.";
    public static final String TYPE = "Error";
}
