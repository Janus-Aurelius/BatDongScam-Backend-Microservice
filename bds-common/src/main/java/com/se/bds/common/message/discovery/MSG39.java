package com.se.bds.common.message.discovery;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG39 {
    public static final String CODE = "MSG 39";
    public static final String MESSAGE = "You cannot review this agent until the interaction is completed.";
    public static final String TYPE = "Error";
}
