package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Per-player rod hold state (not persisted). Tracks bob phase across casts while the rod is equipped. */
public final class FishingRodHoldComponent implements Component<EntityStore> {
    @Nullable
    private static volatile ComponentType<EntityStore, FishingRodHoldComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishingRodHoldComponent.class, FishingRodHoldComponent::new);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishingRodHoldComponent> getComponentType() {
        ComponentType<EntityStore, FishingRodHoldComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishingRodHoldComponent not registered");
        }
        return type;
    }

    private float rodTipBobPhaseRadians;

    public float getRodTipBobPhaseRadians() {
        return rodTipBobPhaseRadians;
    }

    public void setRodTipBobPhaseRadians(float rodTipBobPhaseRadians) {
        this.rodTipBobPhaseRadians = rodTipBobPhaseRadians;
    }

    public void advanceRodTipBobPhase(float deltaRadians) {
        this.rodTipBobPhaseRadians += deltaRadians;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishingRodHoldComponent copy = new FishingRodHoldComponent();
        copy.rodTipBobPhaseRadians = rodTipBobPhaseRadians;
        return copy;
    }
}
