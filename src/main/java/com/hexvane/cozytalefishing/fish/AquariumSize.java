package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Aquarium footprint a fish species is displayed in. */
public enum AquariumSize {
    Small(0),
    Wide2x1(1),
    Tall3x2x2(2);

    private final int tier;

    AquariumSize(int tier) {
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Nullable
    public static AquariumSize fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim()) {
            case "Small" -> Small;
            case "Wide2x1", "Large2x2" -> Wide2x1;
            case "Tall3x2x2" -> Tall3x2x2;
            default -> null;
        };
    }
}
