package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tags a string segment prop for per-viewer visibility filtering. */
public final class FishingLineSegmentComponent implements Component<EntityStore> {
    @Nullable
    private static volatile ComponentType<EntityStore, FishingLineSegmentComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishingLineSegmentComponent.class, FishingLineSegmentComponent::new);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishingLineSegmentComponent> getComponentType() {
        ComponentType<EntityStore, FishingLineSegmentComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishingLineSegmentComponent not registered");
        }
        return type;
    }

    private UUID lineOwnerUuid;
    private int segmentIndex;
    private LineSegmentAudience audience = LineSegmentAudience.THIRD_PERSON_WORLD;

    public FishingLineSegmentComponent() {}

    public FishingLineSegmentComponent(
        @Nonnull UUID lineOwnerUuid,
        int segmentIndex,
        @Nonnull LineSegmentAudience audience
    ) {
        this.lineOwnerUuid = lineOwnerUuid;
        this.segmentIndex = segmentIndex;
        this.audience = audience;
    }

    @Nonnull
    public UUID getLineOwnerUuid() {
        return lineOwnerUuid;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    @Nonnull
    public LineSegmentAudience getAudience() {
        return audience;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new FishingLineSegmentComponent(lineOwnerUuid, segmentIndex, audience);
    }
}
