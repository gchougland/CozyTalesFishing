package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
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
    private boolean submerged;
    private int pokeIndex;

    @Nullable
    private Ref<EntityStore> hookedShadowRef;
    /** Fish shadow currently poking this bobber; blocks other shadows from targeting it. */
    @Nullable
    private Ref<EntityStore> pokingShadowRef;

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

    public boolean isSubmerged() {
        return submerged;
    }

    public void setSubmerged(boolean submerged) {
        this.submerged = submerged;
    }

    public int getPokeIndex() {
        return pokeIndex;
    }

    public void setPokeIndex(int pokeIndex) {
        this.pokeIndex = pokeIndex;
    }

    @Nullable
    public Ref<EntityStore> getHookedShadowRef() {
        return hookedShadowRef;
    }

    public void setHookedShadowRef(@Nullable Ref<EntityStore> hookedShadowRef) {
        this.hookedShadowRef = hookedShadowRef;
    }

    @Nullable
    public Ref<EntityStore> getPokingShadowRef() {
        return pokingShadowRef;
    }

    public void setPokingShadowRef(@Nullable Ref<EntityStore> pokingShadowRef) {
        this.pokingShadowRef = pokingShadowRef;
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
        copy.submerged = submerged;
        copy.pokeIndex = pokeIndex;
        copy.hookedShadowRef = hookedShadowRef;
        copy.pokingShadowRef = pokingShadowRef;
        return copy;
    }
}
