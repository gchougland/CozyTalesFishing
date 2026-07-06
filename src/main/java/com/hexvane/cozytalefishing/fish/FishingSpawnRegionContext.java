package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolved spawn overrides at a block position inside a {@link FishingSpawnRegion}. */
public final class FishingSpawnRegionContext {
    @Nonnull
    private final FishingSpawnRegion region;
    @Nonnull
    private final int[] regionEnvironmentIndices;
    @Nullable
    private final String effectiveZonePrefix;
    @Nullable
    private final String effectiveBiome;
    @Nullable
    private final WaterBodyType waterBodyOverride;

    public FishingSpawnRegionContext(
        @Nonnull FishingSpawnRegion region,
        @Nonnull int[] regionEnvironmentIndices,
        @Nullable String effectiveZonePrefix,
        @Nullable String effectiveBiome,
        @Nullable WaterBodyType waterBodyOverride
    ) {
        this.region = region;
        this.regionEnvironmentIndices = regionEnvironmentIndices;
        this.effectiveZonePrefix = effectiveZonePrefix;
        this.effectiveBiome = effectiveBiome;
        this.waterBodyOverride = waterBodyOverride;
    }

    @Nonnull
    public FishingSpawnRegion getRegion() {
        return region;
    }

    @Nonnull
    public int[] getRegionEnvironmentIndices() {
        return regionEnvironmentIndices;
    }

    @Nullable
    public String getEffectiveZonePrefix() {
        return effectiveZonePrefix;
    }

    @Nullable
    public String getEffectiveBiome() {
        return effectiveBiome;
    }

    @Nullable
    public WaterBodyType getWaterBodyOverride() {
        return waterBodyOverride;
    }

    public boolean isIgnoreWorldZoneGate() {
        return region.isIgnoreWorldZoneGate();
    }
}
