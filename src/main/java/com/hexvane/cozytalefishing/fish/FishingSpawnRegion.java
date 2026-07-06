package com.hexvane.cozytalefishing.fish;

import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Axis-aligned spawn override region for custom handmade water on non-vanilla maps. */
public final class FishingSpawnRegion {
    @Nonnull
    private String id = "";
    @Nullable
    private String name;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    @Nullable
    private String waterBodyType;
    @Nullable
    private String[] environments;
    @Nullable
    private String zoneOverride;
    @Nullable
    private String virtualBiome;
    private boolean ignoreWorldZoneGate = true;

    public FishingSpawnRegion() {}

    @Nonnull
    public String getId() {
        return id;
    }

    public void setId(@Nonnull String id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    @Nullable
    public String getWaterBodyType() {
        return waterBodyType;
    }

    public void setWaterBodyType(@Nullable String waterBodyType) {
        this.waterBodyType = waterBodyType;
    }

    @Nullable
    public String[] getEnvironments() {
        return environments;
    }

    public void setEnvironments(@Nullable String[] environments) {
        this.environments = environments;
    }

    @Nullable
    public String getZoneOverride() {
        return zoneOverride;
    }

    public void setZoneOverride(@Nullable String zoneOverride) {
        this.zoneOverride = zoneOverride;
    }

    @Nullable
    public String getVirtualBiome() {
        return virtualBiome;
    }

    public void setVirtualBiome(@Nullable String virtualBiome) {
        this.virtualBiome = virtualBiome;
    }

    public boolean isIgnoreWorldZoneGate() {
        return ignoreWorldZoneGate;
    }

    public void setIgnoreWorldZoneGate(boolean ignoreWorldZoneGate) {
        this.ignoreWorldZoneGate = ignoreWorldZoneGate;
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public long volume() {
        long dx = (long) maxX - minX + 1;
        long dy = (long) maxY - minY + 1;
        long dz = (long) maxZ - minZ + 1;
        return dx * dy * dz;
    }

    @Nullable
    public WaterBodyType resolveWaterBodyType() {
        return waterBodyType != null ? WaterBodyType.fromString(waterBodyType) : null;
    }

    @Nonnull
    public FishingSpawnRegion copy() {
        FishingSpawnRegion copy = new FishingSpawnRegion();
        copy.id = id;
        copy.name = name;
        copy.minX = minX;
        copy.minY = minY;
        copy.minZ = minZ;
        copy.maxX = maxX;
        copy.maxY = maxY;
        copy.maxZ = maxZ;
        copy.waterBodyType = waterBodyType;
        copy.environments = environments != null ? Arrays.copyOf(environments, environments.length) : null;
        copy.zoneOverride = zoneOverride;
        copy.virtualBiome = virtualBiome;
        copy.ignoreWorldZoneGate = ignoreWorldZoneGate;
        return copy;
    }
}
