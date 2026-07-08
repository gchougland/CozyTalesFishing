package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** How strictly a spawn rule is enforced during species selection. */
public enum FishSpawnRuleMode {
    /** Hard gate — species is excluded when the rule fails. */
    Required,
    /** Soft gate — species stays eligible but spawn weight is adjusted. */
    Preferred,
    /** Rule is not evaluated. */
    Ignored;

    @Nullable
    public static FishSpawnRuleMode fromString(@Nonnull String value) {
        for (FishSpawnRuleMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
