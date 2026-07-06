package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum FishRarity {
    Common(1.0f),
    Uncommon(0.6f),
    Rare(0.35f),
    Epic(0.2f),
    Legendary(0.1f);

    private final float spawnMultiplier;

    FishRarity(float spawnMultiplier) {
        this.spawnMultiplier = spawnMultiplier;
    }

    public float getSpawnMultiplier() {
        return spawnMultiplier;
    }

    @Nullable
    public static FishRarity fromString(@Nonnull String value) {
        for (FishRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(value)) {
                return rarity;
            }
        }
        return null;
    }
}
