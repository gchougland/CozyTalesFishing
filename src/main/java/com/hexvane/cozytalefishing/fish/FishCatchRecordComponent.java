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

    /** @return true when the species was newly discovered */
    public boolean discover(@Nonnull String speciesId) {
        return discoveredSpeciesIds.add(speciesId);
    }

    public void discoverAll(@Nonnull Collection<String> speciesIds) {
        for (String speciesId : speciesIds) {
            if (speciesId != null && !speciesId.isBlank()) {
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
    @Override
    public Component<EntityStore> clone() {
        FishCatchRecordComponent copy = new FishCatchRecordComponent();
        copy.largestSizeCmBySpecies.putAll(largestSizeCmBySpecies);
        copy.discoveredSpeciesIds.addAll(discoveredSpeciesIds);
        return copy;
    }
}
