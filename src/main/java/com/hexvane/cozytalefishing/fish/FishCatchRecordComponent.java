package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** Per-player personal-best fish sizes; persisted in player save data. */
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

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishCatchRecordComponent copy = new FishCatchRecordComponent();
        copy.largestSizeCmBySpecies.putAll(largestSizeCmBySpecies);
        return copy;
    }
}
