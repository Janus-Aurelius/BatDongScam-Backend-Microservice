package com.se.bds.common.message.confirmation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG1 {
    public static final String CODE = "MSG 1";
    public static final String MESSAGE = "Are you sure you want to proceed with this action?";
    public static final String TYPE = "Confirmation";
}
