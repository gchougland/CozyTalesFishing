package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishSpawnLocation {
    @Nonnull
    public static final BuilderCodec<FishSpawnLocation> CODEC =
        BuilderCodec.builder(FishSpawnLocation.class, FishSpawnLocation::new)
            .append(new KeyedCodec<>("Zone", Codec.STRING), (loc, v) -> loc.zone = v, loc -> loc.zone).add()
            .append(
                new KeyedCodec<>("Environments", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (loc, v) -> loc.environments = v,
                loc -> loc.environments
            )
            .add()
            .append(
                new KeyedCodec<>("Biomes", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (loc, v) -> loc.biomes = v,
                loc -> loc.biomes
            )
            .add()
            .build();

    @Nullable
    private String zone;
    @Nullable
    private String[] environments;
    @Nullable
    private String[] biomes;

    @Nullable
    public String getZone() {
        return zone;
    }

    @Nullable
    public String[] getEnvironments() {
        return environments;
    }

    @Nullable
    public String[] getBiomes() {
        return biomes;
    }

    public boolean hasZone() {
        return zone != null && !zone.isBlank();
    }

    public boolean hasEnvironments() {
        return environments != null && environments.length > 0;
    }

    public boolean hasBiomes() {
        return biomes != null && biomes.length > 0;
    }

    void setZone(@Nullable String zone) {
        this.zone = zone;
    }

    void setEnvironments(@Nullable String[] environments) {
        this.environments = environments;
    }

    void setBiomes(@Nullable String[] biomes) {
        this.biomes = biomes;
    }

    @Nonnull
    @Override
    public String toString() {
        return "FishSpawnLocation{zone=" + zone + ", environments=" + Arrays.toString(environments) + ", biomes=" + Arrays.toString(biomes) + '}';
    }
}
