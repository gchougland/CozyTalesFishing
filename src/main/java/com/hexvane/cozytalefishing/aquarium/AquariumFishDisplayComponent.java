package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Marks a fish display prop entity owned by an aquarium block. */
public final class AquariumFishDisplayComponent implements Component<EntityStore> {
    public static final BuilderCodec<AquariumFishDisplayComponent> CODEC =
        BuilderCodec.builder(AquariumFishDisplayComponent.class, AquariumFishDisplayComponent::new)
            .append(new KeyedCodec<>("BlockOrigin", Vector3iUtil.CODEC), (o, v) -> o.blockOrigin = v, o -> o.blockOrigin)
            .add()
            .build();

    @Nullable
    private static ComponentType<EntityStore, AquariumFishDisplayComponent> componentType;

    public static void register(@Nonnull ComponentType<EntityStore, AquariumFishDisplayComponent> type) {
        componentType = type;
    }

    @Nonnull
    public static ComponentType<EntityStore, AquariumFishDisplayComponent> getComponentType() {
        ComponentType<EntityStore, AquariumFishDisplayComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("AquariumFishDisplayComponent not registered");
        }
        return type;
    }

    @Nullable private Vector3i blockOrigin;

    public AquariumFishDisplayComponent() {}

    public AquariumFishDisplayComponent(@Nonnull Vector3i blockOrigin) {
        this.blockOrigin = new Vector3i(blockOrigin);
    }

    @Nullable
    public Vector3i getBlockOrigin() {
        return blockOrigin;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new AquariumFishDisplayComponent(blockOrigin != null ? blockOrigin : new Vector3i());
    }
}
