package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishingSpawnRegionRegistry {
    private static final Map<UUID, List<FishingSpawnRegion>> BY_WORLD = new ConcurrentHashMap<>();

    private FishingSpawnRegionRegistry() {}

    public static void initialize(@Nonnull Path dataDirectory) {
        BY_WORLD.clear();
        Path regionsDir = dataDirectory.resolve("spawn-regions");
        if (!regionsDir.toFile().exists()) {
            return;
        }
        try (var stream = java.nio.file.Files.list(regionsDir)) {
            stream
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String uuidText = fileName.substring(0, fileName.length() - ".json".length());
                    try {
                        UUID worldUuid = UUID.fromString(uuidText);
                        BY_WORLD.put(worldUuid, FishingSpawnRegionStorage.load(path));
                    } catch (IllegalArgumentException | IOException ignored) {
                        // Skip invalid region files.
                    }
                });
        } catch (IOException ignored) {
            // Directory unreadable; regions stay empty until created in-game.
        }
    }

    @Nonnull
    public static List<FishingSpawnRegion> getRegions(@Nonnull UUID worldUuid) {
        return Collections.unmodifiableList(BY_WORLD.getOrDefault(worldUuid, List.of()));
    }

    @Nullable
    public static FishingSpawnRegion getRegion(@Nonnull UUID worldUuid, @Nonnull String id) {
        for (FishingSpawnRegion region : BY_WORLD.getOrDefault(worldUuid, List.of())) {
            if (region.getId().equalsIgnoreCase(id)) {
                return region;
            }
        }
        return null;
    }

    public static boolean upsert(@Nonnull UUID worldUuid, @Nonnull FishingSpawnRegion region) {
        List<FishingSpawnRegion> regions = new ArrayList<>(BY_WORLD.getOrDefault(worldUuid, List.of()));
        regions.removeIf(existing -> existing.getId().equalsIgnoreCase(region.getId()));
        regions.add(region.copy());
        BY_WORLD.put(worldUuid, regions);
        return persist(worldUuid, regions);
    }

    public static boolean remove(@Nonnull UUID worldUuid, @Nonnull String id) {
        List<FishingSpawnRegion> regions = new ArrayList<>(BY_WORLD.getOrDefault(worldUuid, List.of()));
        boolean removed = regions.removeIf(region -> region.getId().equalsIgnoreCase(id));
        if (!removed) {
            return false;
        }
        BY_WORLD.put(worldUuid, regions);
        return persist(worldUuid, regions);
    }

    @Nullable
    public static FishingSpawnRegionContext resolve(@Nonnull UUID worldUuid, int x, int y, int z) {
        if (!FishingModConfig.get().isEnableSpawnRegions()) {
            return null;
        }
        FishingSpawnRegion best = null;
        for (FishingSpawnRegion region : BY_WORLD.getOrDefault(worldUuid, List.of())) {
            if (!region.contains(x, y, z)) {
                continue;
            }
            if (best == null || region.volume() < best.volume()) {
                best = region;
            }
        }
        if (best == null) {
            return null;
        }
        return buildContext(best);
    }

    @Nonnull
    public static FishingSpawnRegionContext buildContext(@Nonnull FishingSpawnRegion region) {
        IntOpenHashSet indices = new IntOpenHashSet();
        if (region.getEnvironments() != null) {
            for (String envId : region.getEnvironments()) {
                int index = Environment.getAssetMap().getIndex(envId);
                if (index != Environment.UNKNOWN_ID) {
                    indices.add(index);
                }
            }
        }
        String zonePrefix = region.getZoneOverride() != null
            ? FishShadowSpawnHelper.extractZonePrefix(region.getZoneOverride())
            : null;
        return new FishingSpawnRegionContext(
            region,
            indices.toIntArray(),
            zonePrefix,
            region.getVirtualBiome(),
            region.resolveWaterBodyType()
        );
    }

    private static boolean persist(@Nonnull UUID worldUuid, @Nonnull List<FishingSpawnRegion> regions) {
        CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
        if (plugin == null) {
            return false;
        }
        Path file = FishingSpawnRegionStorage.worldFile(plugin.getDataDirectory(), worldUuid);
        try {
            FishingSpawnRegionStorage.save(file, regions);
            return true;
        } catch (IOException e) {
            plugin.getLogger().atWarning().withCause(e).log("Failed to save fishing spawn regions for world %s", worldUuid);
            return false;
        }
    }
}
