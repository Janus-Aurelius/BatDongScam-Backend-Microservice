package com.se.bds.common.message.innovation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MSG43 {
    public static final String CODE = "MSG 43";
    public static final String MESSAGE = "Downgrade failed: You have more active listings than allowed by the new plan.";
    public static final String TYPE = "Error";
}
