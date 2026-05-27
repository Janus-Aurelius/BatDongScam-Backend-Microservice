package com.se.bds.common.message.innovation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG42 {
    public static final String CODE = "MSG 42";
    public static final String MESSAGE = "Some fields could not be clearly read. Please verify them manually.";
    public static final String TYPE = "Info";
}
