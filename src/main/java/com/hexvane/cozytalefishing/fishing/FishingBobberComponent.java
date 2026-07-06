package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** State attached to the bobber projectile entity. */
public final class FishingBobberComponent implements Component<EntityStore> {
    @Nullable
    private static volatile ComponentType<EntityStore, FishingBobberComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishingBobberComponent.class, FishingBobberComponent::new);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishingBobberComponent> getComponentType() {
        ComponentType<EntityStore, FishingBobberComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishingBobberComponent not registered");
        }
        return type;
    }

    @Nonnull
    private UUID ownerUuid;

    private FishingBobberPhase phase = FishingBobberPhase.FLYING;
    private boolean splashPlayed;
    private double latchedSurfaceY;
    private float bobPhase;
    private float groundedTimer;

    public FishingBobberComponent(@Nonnull UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    /** Codec / registry default instance (overwritten when spawned). */
    public FishingBobberComponent() {
        this.ownerUuid = new UUID(0L, 0L);
    }

    @Nonnull
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Nonnull
    public FishingBobberPhase getPhase() {
        return phase;
    }

    public void setPhase(@Nonnull FishingBobberPhase phase) {
        this.phase = phase;
    }

    public boolean isSplashPlayed() {
        return splashPlayed;
    }

    public void setSplashPlayed(boolean splashPlayed) {
        this.splashPlayed = splashPlayed;
    }

    public double getLatchedSurfaceY() {
        return latchedSurfaceY;
    }

    public void setLatchedSurfaceY(double latchedSurfaceY) {
        this.latchedSurfaceY = latchedSurfaceY;
    }

    public float getBobPhase() {
        return bobPhase;
    }

    public void setBobPhase(float bobPhase) {
        this.bobPhase = bobPhase;
    }

    public float getGroundedTimer() {
        return groundedTimer;
    }

    public void setGroundedTimer(float groundedTimer) {
        this.groundedTimer = groundedTimer;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishingBobberComponent copy = new FishingBobberComponent(ownerUuid);
        copy.phase = phase;
        copy.splashPlayed = splashPlayed;
        copy.latchedSurfaceY = latchedSurfaceY;
        copy.bobPhase = bobPhase;
        copy.groundedTimer = groundedTimer;
        return copy;
    }
}
