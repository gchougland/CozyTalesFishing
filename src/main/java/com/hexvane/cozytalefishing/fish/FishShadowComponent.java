package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishShadowComponent implements Component<EntityStore> {
    @Nullable
    private static volatile ComponentType<EntityStore, FishShadowComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishShadowComponent.class, FishShadowComponent::new);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishShadowComponent> getComponentType() {
        ComponentType<EntityStore, FishShadowComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishShadowComponent not registered");
        }
        return type;
    }

    @Nonnull
    private String speciesId = "";
    @Nonnull
    private FishShadowType shadowType = FishShadowType.Small;
    @Nonnull
    private FishShadowState state = FishShadowState.WANDERING;
    @Nonnull
    private WaterBodyType waterBodyType = WaterBodyType.Pond;
    /** When fishing in a registered mod fluid (e.g. oil), used for surface movement — not inferred from {@link WaterBodyType}. */
    @Nullable
    private String columnFluidAssetId;

    private int pokeCountRemaining;
    private int pokeTotal;
    private float pokeTimer;
    private float stateTimer;
    private float wanderDirection;
    private float wanderTimer;
    private float fleeTimer;
    /** Seconds without a nearby rod-holder while wandering; triggers shrink/despawn. */
    private float idleTimer;
    private float baseScale = 1.0f;
    private float currentScale = 1.0f;

    @Nonnull
    private FishShadowPokePhase pokePhase = FishShadowPokePhase.LURK;
    private double bobberAnchorX;
    private double bobberAnchorZ;
    private boolean bobberAnchorInitialized;
    /** Horizontal fish-to-player distance when the fight started; escape uses extra run beyond this. */
    private float fightStartPlayerDistance = -1.0f;

    @Nullable
    private Ref<EntityStore> targetBobberRef;
    @Nullable
    private UUID targetBobberOwnerUuid;
    @Nullable
    private UUID hookedPlayerUuid;

    @Nonnull
    public String getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(@Nonnull String speciesId) {
        this.speciesId = speciesId;
    }

    @Nonnull
    public FishShadowType getShadowType() {
        return shadowType;
    }

    public void setShadowType(@Nonnull FishShadowType shadowType) {
        this.shadowType = shadowType;
    }

    @Nonnull
    public FishShadowState getState() {
        return state;
    }

    public void setState(@Nonnull FishShadowState state) {
        this.state = state;
    }

    @Nonnull
    public WaterBodyType getWaterBodyType() {
        return waterBodyType;
    }

    public void setWaterBodyType(@Nonnull WaterBodyType waterBodyType) {
        this.waterBodyType = waterBodyType;
    }

    @Nullable
    public String getColumnFluidAssetId() {
        return columnFluidAssetId;
    }

    public void setColumnFluidAssetId(@Nullable String columnFluidAssetId) {
        this.columnFluidAssetId = columnFluidAssetId;
    }

    public int getPokeCountRemaining() {
        return pokeCountRemaining;
    }

    public void setPokeCountRemaining(int pokeCountRemaining) {
        this.pokeCountRemaining = pokeCountRemaining;
    }

    public int getPokeTotal() {
        return pokeTotal;
    }

    public void setPokeTotal(int pokeTotal) {
        this.pokeTotal = pokeTotal;
    }

    public float getPokeTimer() {
        return pokeTimer;
    }

    public void setPokeTimer(float pokeTimer) {
        this.pokeTimer = pokeTimer;
    }

    public float getStateTimer() {
        return stateTimer;
    }

    public void setStateTimer(float stateTimer) {
        this.stateTimer = stateTimer;
    }

    public float getWanderDirection() {
        return wanderDirection;
    }

    public void setWanderDirection(float wanderDirection) {
        this.wanderDirection = wanderDirection;
    }

    public float getWanderTimer() {
        return wanderTimer;
    }

    public void setWanderTimer(float wanderTimer) {
        this.wanderTimer = wanderTimer;
    }

    public float getFleeTimer() {
        return fleeTimer;
    }

    public void setFleeTimer(float fleeTimer) {
        this.fleeTimer = fleeTimer;
    }

    public float getIdleTimer() {
        return idleTimer;
    }

    public void setIdleTimer(float idleTimer) {
        this.idleTimer = idleTimer;
    }

    public float getBaseScale() {
        return baseScale;
    }

    public void setBaseScale(float baseScale) {
        this.baseScale = baseScale;
        this.currentScale = baseScale;
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public void setCurrentScale(float currentScale) {
        this.currentScale = currentScale;
    }

    @Nullable
    public Ref<EntityStore> getTargetBobberRef() {
        return targetBobberRef;
    }

    public void setTargetBobberRef(@Nullable Ref<EntityStore> targetBobberRef) {
        this.targetBobberRef = targetBobberRef;
    }

    @Nullable
    public UUID getTargetBobberOwnerUuid() {
        return targetBobberOwnerUuid;
    }

    public void setTargetBobberOwnerUuid(@Nullable UUID targetBobberOwnerUuid) {
        this.targetBobberOwnerUuid = targetBobberOwnerUuid;
    }

    @Nullable
    public UUID getHookedPlayerUuid() {
        return hookedPlayerUuid;
    }

    public void setHookedPlayerUuid(@Nullable UUID hookedPlayerUuid) {
        this.hookedPlayerUuid = hookedPlayerUuid;
    }

    @Nonnull
    public FishShadowPokePhase getPokePhase() {
        return pokePhase;
    }

    public void setPokePhase(@Nonnull FishShadowPokePhase pokePhase) {
        this.pokePhase = pokePhase;
    }

    public double getBobberAnchorX() {
        return bobberAnchorX;
    }

    public double getBobberAnchorZ() {
        return bobberAnchorZ;
    }

    public boolean isBobberAnchorInitialized() {
        return bobberAnchorInitialized;
    }

    public void setBobberAnchor(double x, double z) {
        this.bobberAnchorX = x;
        this.bobberAnchorZ = z;
        this.bobberAnchorInitialized = true;
    }

    public void clearBobberAnchor() {
        this.bobberAnchorInitialized = false;
    }

    public float getFightStartPlayerDistance() {
        return fightStartPlayerDistance;
    }

    public void setFightStartPlayerDistance(float fightStartPlayerDistance) {
        this.fightStartPlayerDistance = fightStartPlayerDistance;
    }

    public void clearFightStartPlayerDistance() {
        this.fightStartPlayerDistance = -1.0f;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishShadowComponent copy = new FishShadowComponent();
        copy.speciesId = speciesId;
        copy.shadowType = shadowType;
        copy.state = state;
        copy.waterBodyType = waterBodyType;
        copy.columnFluidAssetId = columnFluidAssetId;
        copy.pokeCountRemaining = pokeCountRemaining;
        copy.pokeTotal = pokeTotal;
        copy.pokeTimer = pokeTimer;
        copy.stateTimer = stateTimer;
        copy.wanderDirection = wanderDirection;
        copy.wanderTimer = wanderTimer;
        copy.fleeTimer = fleeTimer;
        copy.idleTimer = idleTimer;
        copy.baseScale = baseScale;
        copy.currentScale = currentScale;
        copy.targetBobberRef = targetBobberRef;
        copy.targetBobberOwnerUuid = targetBobberOwnerUuid;
        copy.hookedPlayerUuid = hookedPlayerUuid;
        copy.pokePhase = pokePhase;
        copy.bobberAnchorX = bobberAnchorX;
        copy.bobberAnchorZ = bobberAnchorZ;
        copy.bobberAnchorInitialized = bobberAnchorInitialized;
        copy.fightStartPlayerDistance = fightStartPlayerDistance;
        return copy;
    }
}
