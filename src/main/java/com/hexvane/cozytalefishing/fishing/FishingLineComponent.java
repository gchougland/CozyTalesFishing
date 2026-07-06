package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Per-player active fishing line simulation state (not persisted). */
public final class FishingLineComponent implements Component<EntityStore> {
    @Nullable
    private static volatile ComponentType<EntityStore, FishingLineComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishingLineComponent.class, FishingLineComponent::new);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishingLineComponent> getComponentType() {
        ComponentType<EntityStore, FishingLineComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishingLineComponent not registered");
        }
        return type;
    }

    private FishingLinePhase phase = FishingLinePhase.INACTIVE;
    private float chargeNormalized;
    private float maxLength;
    private int whipTicksRemaining;
    private boolean loggedStretchDebug;
    private boolean castSetupPending;

    @Nullable
    private Ref<EntityStore> bobberRef;

    @Nonnull
    private final Vector3d[] nodePositions = new Vector3d[FishingConstants.NODE_COUNT];

    @Nonnull
    private final Vector3d[] nodeOldPositions = new Vector3d[FishingConstants.NODE_COUNT];

    @Nonnull
    @SuppressWarnings("unchecked")
    private final Ref<EntityStore>[] segmentRefs = new Ref[FishingConstants.SEGMENT_COUNT];

    public FishingLineComponent() {
        for (int i = 0; i < FishingConstants.NODE_COUNT; i++) {
            nodePositions[i] = new Vector3d();
            nodeOldPositions[i] = new Vector3d();
        }
    }

    public boolean isActive() {
        return phase != FishingLinePhase.INACTIVE && phase != FishingLinePhase.RECALLING;
    }

    @Nonnull
    public FishingLinePhase getPhase() {
        return phase;
    }

    public void setPhase(@Nonnull FishingLinePhase phase) {
        this.phase = phase;
    }

    public float getChargeNormalized() {
        return chargeNormalized;
    }

    public void setChargeNormalized(float chargeNormalized) {
        this.chargeNormalized = chargeNormalized;
    }

    public float getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(float maxLength) {
        this.maxLength = maxLength;
    }

    public int getWhipTicksRemaining() {
        return whipTicksRemaining;
    }

    public void setWhipTicksRemaining(int whipTicksRemaining) {
        this.whipTicksRemaining = whipTicksRemaining;
    }

    public boolean isLoggedStretchDebug() {
        return loggedStretchDebug;
    }

    public void setLoggedStretchDebug(boolean loggedStretchDebug) {
        this.loggedStretchDebug = loggedStretchDebug;
    }

    public boolean isCastSetupPending() {
        return castSetupPending;
    }

    public void setCastSetupPending(boolean castSetupPending) {
        this.castSetupPending = castSetupPending;
    }

    @Nullable
    public Ref<EntityStore> getBobberRef() {
        return bobberRef;
    }

    public void setBobberRef(@Nullable Ref<EntityStore> bobberRef) {
        this.bobberRef = bobberRef;
    }

    @Nonnull
    public Vector3d[] getNodePositions() {
        return nodePositions;
    }

    @Nonnull
    public Vector3d[] getNodeOldPositions() {
        return nodeOldPositions;
    }

    @Nonnull
    public Ref<EntityStore>[] getSegmentRefs() {
        return segmentRefs;
    }

    public void reset() {
        phase = FishingLinePhase.INACTIVE;
        chargeNormalized = 0.0f;
        maxLength = 0.0f;
        whipTicksRemaining = 0;
        loggedStretchDebug = false;
        castSetupPending = false;
        bobberRef = null;
        for (int i = 0; i < segmentRefs.length; i++) {
            segmentRefs[i] = null;
        }
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishingLineComponent copy = new FishingLineComponent();
        copy.phase = phase;
        copy.chargeNormalized = chargeNormalized;
        copy.maxLength = maxLength;
        copy.whipTicksRemaining = whipTicksRemaining;
        copy.loggedStretchDebug = loggedStretchDebug;
        copy.castSetupPending = castSetupPending;
        copy.bobberRef = bobberRef;
        for (int i = 0; i < FishingConstants.NODE_COUNT; i++) {
            copy.nodePositions[i].set(nodePositions[i]);
            copy.nodeOldPositions[i].set(nodeOldPositions[i]);
        }
        System.arraycopy(segmentRefs, 0, copy.segmentRefs, 0, segmentRefs.length);
        return copy;
    }
}
