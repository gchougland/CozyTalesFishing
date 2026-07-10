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

/** Marks a decoration display prop spawned inside an aquarium. */
public final class AquariumDecorationDisplayComponent implements Component<EntityStore> {
    public static final BuilderCodec<AquariumDecorationDisplayComponent> CODEC =
        BuilderCodec.builder(AquariumDecorationDisplayComponent.class, AquariumDecorationDisplayComponent::new)
            .append(new KeyedCodec<>("BlockOrigin", Vector3iUtil.CODEC), (o, v) -> o.blockOrigin = v, o -> o.blockOrigin)
            .add()
            .append(new KeyedCodec<>("SlotIndex", com.hypixel.hytale.codec.Codec.INTEGER), (o, v) -> o.slotIndex = v, o -> o.slotIndex)
            .add()
            .build();

    @Nullable
    private static ComponentType<EntityStore, AquariumDecorationDisplayComponent> componentType;

    public static void register(@Nonnull ComponentType<EntityStore, AquariumDecorationDisplayComponent> type) {
        componentType = type;
    }

    @Nonnull
    public static ComponentType<EntityStore, AquariumDecorationDisplayComponent> getComponentType() {
        ComponentType<EntityStore, AquariumDecorationDisplayComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("AquariumDecorationDisplayComponent not registered");
        }
        return type;
    }

    @Nullable private Vector3i blockOrigin;
    private int slotIndex;

    public AquariumDecorationDisplayComponent() {}

    public AquariumDecorationDisplayComponent(@Nonnull Vector3i blockOrigin, int slotIndex) {
        this.blockOrigin = new Vector3i(blockOrigin);
        this.slotIndex = slotIndex;
    }

    @Nullable
    public Vector3i getBlockOrigin() {
        return blockOrigin;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new AquariumDecorationDisplayComponent(
            blockOrigin != null ? new Vector3i(blockOrigin) : new Vector3i(),
            slotIndex
        );
    }
}
