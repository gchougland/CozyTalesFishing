package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum WaterBodyType {
    Ocean,
    River,
    Pond;

    @Nullable
    public static WaterBodyType fromString(@Nonnull String value) {
        for (WaterBodyType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
