package com.hexvane.cozytalefishing.fish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Maps fluid asset ids to fishable habitats (from JSON assets and optional Java registration). */
public final class FishableFluidRegistry {
    public record Entry(@Nonnull String habitatId, @Nullable String journalHabitatKey) {}

    private static final Map<String, Entry> BY_FLUID_ID_LOWER = new HashMap<>();
    private static final Map<String, Entry> BY_HABITAT_ID_LOWER = new HashMap<>();
    private static final Map<String, Entry> RUNTIME_BY_FLUID_ID_LOWER = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private FishableFluidRegistry() {}

    public static void rebuild() {
        BY_FLUID_ID_LOWER.clear();
        BY_HABITAT_ID_LOWER.clear();

        for (FishableFluidAsset asset : FishableFluidAsset.getAssetMap().getAssetMap().values()) {
            if (asset == null) {
                continue;
            }
            Entry entry = new Entry(asset.getHabitatId(), asset.getJournalHabitatKey());
            BY_HABITAT_ID_LOWER.put(normalizeKey(asset.getHabitatId()), entry);
            for (String fluidId : asset.getFluidIds()) {
                if (fluidId == null || fluidId.isBlank()) {
                    continue;
                }
                BY_FLUID_ID_LOWER.put(normalizeKey(fluidId), entry);
            }
        }

        for (Map.Entry<String, Entry> runtime : RUNTIME_BY_FLUID_ID_LOWER.entrySet()) {
            BY_FLUID_ID_LOWER.put(runtime.getKey(), runtime.getValue());
            BY_HABITAT_ID_LOWER.putIfAbsent(normalizeKey(runtime.getValue().habitatId()), runtime.getValue());
        }

        initialized = true;
    }

    public static void ensureInitialized() {
        if (!initialized) {
            rebuild();
        }
    }

    /** Registers a fishable fluid at runtime (merged on next {@link #rebuild()}). */
    public static void register(
        @Nonnull String fluidAssetId,
        @Nonnull String habitatId,
        @Nullable String journalHabitatKey
    ) {
        Entry entry = new Entry(habitatId, journalHabitatKey);
        RUNTIME_BY_FLUID_ID_LOWER.put(normalizeKey(fluidAssetId), entry);
        rebuild();
    }

    public static void unregister(@Nonnull String fluidAssetId) {
        RUNTIME_BY_FLUID_ID_LOWER.remove(normalizeKey(fluidAssetId));
        rebuild();
    }

    public static boolean isRegisteredFluidId(@Nullable String fluidAssetId) {
        if (fluidAssetId == null || fluidAssetId.isBlank()) {
            return false;
        }
        ensureInitialized();
        return BY_FLUID_ID_LOWER.containsKey(normalizeKey(fluidAssetId));
    }

    public static boolean isFishableFluidId(int fluidIndex) {
        String id = fluidAssetIdFromIndex(fluidIndex);
        if (id == null) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("lava") || lower.contains("water")) {
            return true;
        }
        return isRegisteredFluidId(id);
    }

    @Nullable
    public static String fluidAssetIdFromIndex(int fluidIndex) {
        if (fluidIndex == com.hypixel.hytale.server.core.asset.type.fluid.Fluid.EMPTY_ID) {
            return null;
        }
        var fluid = com.hypixel.hytale.server.core.asset.type.fluid.Fluid.getAssetMap().getAsset(fluidIndex);
        if (fluid == null || fluid.getId() == null) {
            return null;
        }
        return fluid.getId();
    }

    @Nullable
    public static Entry entryForFluidId(@Nullable String fluidAssetId) {
        if (fluidAssetId == null || fluidAssetId.isBlank()) {
            return null;
        }
        ensureInitialized();
        return BY_FLUID_ID_LOWER.get(normalizeKey(fluidAssetId));
    }

    @Nullable
    public static Entry entryForHabitatId(@Nullable String habitatId) {
        if (habitatId == null || habitatId.isBlank()) {
            return null;
        }
        ensureInitialized();
        return BY_HABITAT_ID_LOWER.get(normalizeKey(habitatId));
    }

    /** True when {@code ruleId} matches {@code columnFluidId} or shares the same habitat. */
    public static boolean matchesFluidRule(@Nonnull String ruleId, @Nonnull String columnFluidId) {
        if (ruleId.equalsIgnoreCase(columnFluidId)) {
            return true;
        }
        Entry ruleEntry = entryForFluidId(ruleId);
        if (ruleEntry != null && ruleEntry.habitatId().equalsIgnoreCase(ruleId)) {
            return true;
        }
        Entry columnEntry = entryForFluidId(columnFluidId);
        if (columnEntry != null && columnEntry.habitatId().equalsIgnoreCase(ruleId)) {
            return true;
        }
        if (ruleEntry != null && columnEntry != null) {
            return ruleEntry.habitatId().equalsIgnoreCase(columnEntry.habitatId());
        }
        return false;
    }

    @Nonnull
    public static String formatJournalHabitat(@Nullable String fluidAssetId) {
        Entry entry = entryForFluidId(fluidAssetId);
        if (entry == null || entry.journalHabitatKey() == null || entry.journalHabitatKey().isBlank()) {
            return fluidAssetId != null ? fluidAssetId : "—";
        }
        return entry.journalHabitatKey();
    }

    @Nonnull
    public static List<String> registeredFluidIdsSnapshot() {
        ensureInitialized();
        return Collections.unmodifiableList(new ArrayList<>(BY_FLUID_ID_LOWER.keySet()));
    }

    /** One row per {@link FishableFluidAsset} for journal habitat filters (deduped by habitat id). */
    @Nonnull
    public static List<JournalHabitatFilter> journalHabitatFilters() {
        ensureInitialized();
        Map<String, JournalHabitatFilter> byHabitat = new HashMap<>();
        for (FishableFluidAsset asset : FishableFluidAsset.getAssetMap().getAssetMap().values()) {
            if (asset == null) {
                continue;
            }
            String habitatId = asset.getHabitatId();
            byHabitat.putIfAbsent(
                normalizeKey(habitatId),
                new JournalHabitatFilter(habitatId, asset.getJournalHabitatKey())
            );
        }
        List<JournalHabitatFilter> filters = new ArrayList<>(byHabitat.values());
        filters.sort(Comparator.comparing(JournalHabitatFilter::habitatId, String.CASE_INSENSITIVE_ORDER));
        return filters;
    }

    public record JournalHabitatFilter(@Nonnull String habitatId, @Nullable String journalHabitatKey) {}

    @Nonnull
    private static String normalizeKey(@Nonnull String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
