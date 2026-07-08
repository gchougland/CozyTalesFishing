package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/** Per-player fish journal data: discoveries and personal-best sizes; persisted in player save data. */
public final class FishCatchRecordComponent implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<FishCatchRecordComponent> CODEC =
        BuilderCodec.builder(FishCatchRecordComponent.class, FishCatchRecordComponent::new)
            .append(
                new KeyedCodec<>("LargestSizeCmBySpecies", new MapCodec<>(Codec.FLOAT, HashMap::new, false)),
                (component, sizes) -> {
                    component.largestSizeCmBySpecies.clear();
                    if (sizes != null) {
                        component.largestSizeCmBySpecies.putAll(sizes);
                    }
                },
                component -> new HashMap<>(component.largestSizeCmBySpecies)
            )
            .add()
            .append(
                new KeyedCodec<>("DiscoveredSpeciesIds", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (component, ids) -> {
                    component.discoveredSpeciesIds.clear();
                    if (ids != null) {
                        for (String id : ids) {
                            if (id != null && !id.isBlank()) {
                                component.discoveredSpeciesIds.add(id);
                            }
                        }
                    }
                },
                component -> component.discoveredSpeciesIds.toArray(String[]::new)
            )
            .add()
            .append(
                new KeyedCodec<>("HintedSpeciesIds", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (component, ids) -> {
                    component.hintedSpeciesIds.clear();
                    if (ids != null) {
                        for (String id : ids) {
                            if (id != null && !id.isBlank()) {
                                component.hintedSpeciesIds.add(id);
                            }
                        }
                    }
                },
                component -> component.hintedSpeciesIds.toArray(String[]::new)
            )
            .add()
            .append(
                new KeyedCodec<>("LeaderboardDisplayName", Codec.STRING),
                (component, name) -> component.leaderboardDisplayName = name != null ? name : "",
                component -> component.leaderboardDisplayName
            )
            .add()
            .append(
                new KeyedCodec<>("CatchCountBySpecies", new MapCodec<>(Codec.INTEGER, HashMap::new, false)),
                (component, counts) -> {
                    component.catchCountBySpecies.clear();
                    if (counts != null) {
                        component.catchCountBySpecies.putAll(counts);
                    }
                },
                component -> new HashMap<>(component.catchCountBySpecies)
            )
            .add()
            .build();

    @Nonnull
    private static volatile ComponentType<EntityStore, FishCatchRecordComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishCatchRecordComponent.class, "CozyFishCatchRecords", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishCatchRecordComponent> getComponentType() {
        ComponentType<EntityStore, FishCatchRecordComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishCatchRecordComponent not registered");
        }
        return type;
    }

    @Nonnull
    private final Map<String, Float> largestSizeCmBySpecies = new HashMap<>();

    @Nonnull
    private final Set<String> discoveredSpeciesIds = new HashSet<>();

    @Nonnull
    private final Set<String> hintedSpeciesIds = new HashSet<>();

    @Nonnull
    private String leaderboardDisplayName = "";

    @Nonnull
    private final Map<String, Integer> catchCountBySpecies = new HashMap<>();

    public float getLargestSizeCm(@Nonnull String speciesId) {
        return largestSizeCmBySpecies.getOrDefault(speciesId, 0.0f);
    }

    public boolean updateLargest(@Nonnull String speciesId, float sizeCm) {
        float current = getLargestSizeCm(speciesId);
        if (sizeCm <= current) {
            return false;
        }
        largestSizeCmBySpecies.put(speciesId, sizeCm);
        return true;
    }

    public boolean isDiscovered(@Nonnull String speciesId) {
        return discoveredSpeciesIds.contains(speciesId);
    }

    public boolean isHinted(@Nonnull String speciesId) {
        return hintedSpeciesIds.contains(speciesId);
    }

    /** @return true when the species was newly discovered */
    public boolean discover(@Nonnull String speciesId) {
        hintedSpeciesIds.remove(speciesId);
        return discoveredSpeciesIds.add(speciesId);
    }

    /** @return true when the species was newly hinted */
    public boolean hint(@Nonnull String speciesId) {
        if (discoveredSpeciesIds.contains(speciesId)) {
            return false;
        }
        return hintedSpeciesIds.add(speciesId);
    }

    public void hintAll(@Nonnull Collection<String> speciesIds) {
        for (String speciesId : speciesIds) {
            if (speciesId != null && !speciesId.isBlank() && !discoveredSpeciesIds.contains(speciesId)) {
                hintedSpeciesIds.add(speciesId);
            }
        }
    }

    @Nonnull
    public Set<String> getHintedSpeciesIds() {
        return Set.copyOf(hintedSpeciesIds);
    }

    public void discoverAll(@Nonnull Collection<String> speciesIds) {
        for (String speciesId : speciesIds) {
            if (speciesId != null && !speciesId.isBlank()) {
                hintedSpeciesIds.remove(speciesId);
                discoveredSpeciesIds.add(speciesId);
            }
        }
    }

    @Nonnull
    public Set<String> getDiscoveredSpeciesIds() {
        return Set.copyOf(discoveredSpeciesIds);
    }

    public int getDiscoveredCount() {
        return discoveredSpeciesIds.size();
    }

    @Nonnull
    public Map<String, Float> getLargestSizesBySpecies() {
        return Map.copyOf(largestSizeCmBySpecies);
    }

    public int getCatchCount(@Nonnull String speciesId) {
        return catchCountBySpecies.getOrDefault(speciesId, 0);
    }

    /**
     * Returns the persisted catch count, or 1 for discovered species that predate catch tracking.
     */
    public int getEffectiveCatchCount(@Nonnull String speciesId) {
        int stored = getCatchCount(speciesId);
        if (stored > 0) {
            return stored;
        }
        if (isDiscovered(speciesId)) {
            return 1;
        }
        return 0;
    }

    public void incrementCatchCount(@Nonnull String speciesId) {
        catchCountBySpecies.merge(speciesId, 1, Integer::sum);
    }

    public int getTotalCatchCount() {
        int total = 0;
        for (String speciesId : discoveredSpeciesIds) {
            total += getEffectiveCatchCount(speciesId);
        }
        return total;
    }

    @Nonnull
    public Map<String, Integer> getCatchCountsBySpecies() {
        return Map.copyOf(catchCountBySpecies);
    }

    @Nonnull
    public String getLeaderboardDisplayName() {
        return leaderboardDisplayName;
    }

    /** @return true when the display name was updated */
    public boolean updateDisplayName(@Nonnull String name) {
        if (name.isBlank() || name.equals(leaderboardDisplayName)) {
            return false;
        }
        leaderboardDisplayName = name;
        return true;
    }

    public void clear() {
        largestSizeCmBySpecies.clear();
        discoveredSpeciesIds.clear();
        hintedSpeciesIds.clear();
        catchCountBySpecies.clear();
        leaderboardDisplayName = "";
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishCatchRecordComponent copy = new FishCatchRecordComponent();
        copy.largestSizeCmBySpecies.putAll(largestSizeCmBySpecies);
        copy.discoveredSpeciesIds.addAll(discoveredSpeciesIds);
        copy.hintedSpeciesIds.addAll(hintedSpeciesIds);
        copy.leaderboardDisplayName = leaderboardDisplayName;
        copy.catchCountBySpecies.putAll(catchCountBySpecies);
        return copy;
    }
}
