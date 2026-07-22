package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishSpeciesRegistry {
    private static final IntSet OCEAN_ENVIRONMENT_INDICES = new IntOpenHashSet();
    private static final Int2ObjectMap<String> ENVIRONMENT_ZONE_PREFIX = new Int2ObjectOpenHashMap<>();
    private static final Map<WaterBodyType, List<FishSpeciesAsset>> BY_WATER_BODY = new EnumMap<>(WaterBodyType.class);
    private static final List<FishSpeciesAsset> TRASH_SPECIES = new ArrayList<>();
    private static final List<FishSpeciesAsset> TREASURE_SPECIES = new ArrayList<>();
    private static final List<FishSpeciesAsset> MONSTER_SPECIES = new ArrayList<>();
    private static final List<FishSpeciesAsset> JOURNAL_SPECIES = new ArrayList<>();
    private static final Map<String, FishSpeciesAsset> BY_ID = new HashMap<>();
    private static final Map<String, FishSpeciesAsset> BY_ITEM_ID = new HashMap<>();
    private static final Map<String, List<FishSpeciesAsset>> BY_FLUID_KEY = new HashMap<>();
    private static volatile boolean initialized;

    private FishSpeciesRegistry() {}

    public static void rebuild() {
        FishableFluidRegistry.rebuild();
        OCEAN_ENVIRONMENT_INDICES.clear();
        ENVIRONMENT_ZONE_PREFIX.clear();
        BY_WATER_BODY.clear();
        TRASH_SPECIES.clear();
        TREASURE_SPECIES.clear();
        MONSTER_SPECIES.clear();
        JOURNAL_SPECIES.clear();
        BY_ID.clear();
        BY_ITEM_ID.clear();
        BY_FLUID_KEY.clear();
        for (WaterBodyType type : WaterBodyType.values()) {
            BY_WATER_BODY.put(type, new ArrayList<>());
        }

        for (Environment env : Environment.getAssetMap().getAssetMap().values()) {
            if (env == null || env.getId() == null) {
                continue;
            }
            String envId = env.getId();
            int index = Environment.getAssetMap().getIndex(envId);
            if (index == Environment.UNKNOWN_ID) {
                continue;
            }
            String zonePrefix = FishShadowSpawnHelper.extractZonePrefix(envId);
            if (zonePrefix != null) {
                ENVIRONMENT_ZONE_PREFIX.put(index, zonePrefix);
            }
            if (envId.contains("Shores") || envId.contains("Shallow_Ocean")) {
                OCEAN_ENVIRONMENT_INDICES.add(index);
            }
        }

        DefaultAssetMap<String, FishSpeciesAsset> speciesMap = FishSpeciesAsset.getAssetMap();
        for (FishSpeciesAsset species : speciesMap.getAssetMap().values()) {
            if (species == null) {
                continue;
            }
            species.allowedEnvironmentIndices = resolveEnvironmentIndices(species);
            BY_ID.put(species.getId(), species);
            if (species.getItemId() != null && !species.getItemId().isBlank()) {
                BY_ITEM_ID.put(species.getItemId(), species);
            }
            if (species.isTrash()) {
                TRASH_SPECIES.add(species);
            } else if (species.isTreasure()) {
                TREASURE_SPECIES.add(species);
            } else if (species.isMonster()) {
                MONSTER_SPECIES.add(species);
            } else {
                JOURNAL_SPECIES.add(species);
                if (species.getSpawnRules().indexesByFluid()) {
                    indexJournalSpeciesByFluid(species);
                } else {
                    for (WaterBodyType bodyType : species.getWaterBodyTypes()) {
                        BY_WATER_BODY.get(bodyType).add(species);
                    }
                }
            }
        }
        initialized = true;
    }

    public static void ensureInitialized() {
        if (!initialized) {
            rebuild();
        }
    }

    @Nonnull
    public static IntSet getOceanEnvironmentIndices() {
        ensureInitialized();
        return OCEAN_ENVIRONMENT_INDICES;
    }

    @Nonnull
    public static List<FishSpeciesAsset> getSpeciesForWaterBody(@Nonnull WaterBodyType bodyType) {
        ensureInitialized();
        return BY_WATER_BODY.getOrDefault(bodyType, List.of());
    }

    @Nonnull
    public static List<FishSpeciesAsset> getSpeciesForFluid(@Nullable String columnFluidAssetId) {
        ensureInitialized();
        if (columnFluidAssetId == null || columnFluidAssetId.isBlank()) {
            return List.of();
        }
        List<FishSpeciesAsset> direct = BY_FLUID_KEY.get(normalizeFluidKey(columnFluidAssetId));
        if (direct != null && !direct.isEmpty()) {
            return new ArrayList<>(direct);
        }
        FishableFluidRegistry.Entry entry = FishableFluidRegistry.entryForFluidId(columnFluidAssetId);
        if (entry == null) {
            return List.of();
        }
        List<FishSpeciesAsset> byHabitat = BY_FLUID_KEY.get(normalizeFluidKey(entry.habitatId()));
        return byHabitat != null ? new ArrayList<>(byHabitat) : List.of();
    }

    private static void indexJournalSpeciesByFluid(@Nonnull FishSpeciesAsset species) {
        FishSpawnRules.FluidRule fluidRule = species.getSpawnRules().getFluid();
        if (!fluidRule.hasIds()) {
            return;
        }
        for (String id : fluidRule.getIds()) {
            if (id == null || id.isBlank()) {
                continue;
            }
            addFluidIndexKey(id, species);
            FishableFluidRegistry.Entry entry = FishableFluidRegistry.entryForFluidId(id);
            if (entry != null) {
                addFluidIndexKey(entry.habitatId(), species);
            }
        }
    }

    private static void addFluidIndexKey(@Nonnull String key, @Nonnull FishSpeciesAsset species) {
        String normalized = normalizeFluidKey(key);
        List<FishSpeciesAsset> list = BY_FLUID_KEY.computeIfAbsent(normalized, ignored -> new ArrayList<>());
        if (list.stream().noneMatch(existing -> existing.getId().equals(species.getId()))) {
            list.add(species);
        }
    }

    @Nonnull
    private static String normalizeFluidKey(@Nonnull String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static FishSpeciesAsset getSpecies(@Nonnull String id) {
        ensureInitialized();
        return BY_ID.get(id);
    }

    @Nullable
    public static FishSpeciesAsset getSpeciesByItemId(@Nonnull String itemId) {
        ensureInitialized();
        return BY_ITEM_ID.get(itemId);
    }

    @Nonnull
    public static List<FishSpeciesAsset> getAllSpecies() {
        ensureInitialized();
        return new ArrayList<>(BY_ID.values());
    }

    @Nonnull
    public static List<FishSpeciesAsset> getTrashSpecies() {
        ensureInitialized();
        return new ArrayList<>(TRASH_SPECIES);
    }

    @Nonnull
    public static List<FishSpeciesAsset> getTreasureSpecies() {
        ensureInitialized();
        return new ArrayList<>(TREASURE_SPECIES);
    }

    @Nonnull
    public static List<FishSpeciesAsset> getMonsterSpecies() {
        ensureInitialized();
        return new ArrayList<>(MONSTER_SPECIES);
    }

    @Nonnull
    public static List<FishSpeciesAsset> getJournalSpecies() {
        ensureInitialized();
        return new ArrayList<>(JOURNAL_SPECIES);
    }

    @Nullable
    public static String getEnvironmentZonePrefix(int environmentIndex) {
        ensureInitialized();
        return ENVIRONMENT_ZONE_PREFIX.get(environmentIndex);
    }

    @Nullable
    public static String getPrimaryZonePrefix(@Nonnull FishSpeciesAsset species) {
        ensureInitialized();
        FishSpawnLocation location = species.getSpawnLocation();
        String sharedZone = null;
        if (location.hasEnvironments()) {
            for (String envId : location.getEnvironments()) {
                String zone = FishShadowSpawnHelper.extractZonePrefix(envId);
                if (zone == null) {
                    continue;
                }
                if (sharedZone == null) {
                    sharedZone = zone;
                } else if (!sharedZone.equalsIgnoreCase(zone)) {
                    return null;
                }
            }
        }
        return sharedZone;
    }

    public static boolean requiresUndergroundSpawn(@Nonnull FishSpeciesAsset species) {
        ensureInitialized();
        if (species.isUndergroundOnly()) {
            return true;
        }
        FishSpawnLocation location = species.getSpawnLocation();
        if (location.hasZone() && "Zone4".equalsIgnoreCase(location.getZone())) {
            return true;
        }
        if (location.hasEnvironments()) {
            for (String envId : location.getEnvironments()) {
                if (envId != null && envId.contains("Zone4")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] resolveEnvironmentIndices(@Nonnull FishSpeciesAsset species) {
        FishSpawnLocation location = species.getSpawnLocation();
        IntOpenHashSet indices = new IntOpenHashSet();
        if (location.hasZone()) {
            String zonePrefix = "Env_" + location.getZone() + "_";
            for (Environment env : Environment.getAssetMap().getAssetMap().values()) {
                if (env != null && env.getId() != null && env.getId().startsWith(zonePrefix)) {
                    int index = Environment.getAssetMap().getIndex(env.getId());
                    if (index != Environment.UNKNOWN_ID) {
                        indices.add(index);
                    }
                }
            }
        }
        if (location.hasEnvironments()) {
            for (String envId : location.getEnvironments()) {
                int index = Environment.getAssetMap().getIndex(envId);
                if (index != Environment.UNKNOWN_ID) {
                    indices.add(index);
                }
            }
        }
        if (location.hasBiomes()) {
            for (String biomeId : location.getBiomes()) {
                int index = Environment.getAssetMap().getIndex(biomeId);
                if (index != Environment.UNKNOWN_ID) {
                    indices.add(index);
                }
            }
        }
        return indices.toIntArray();
    }
}
