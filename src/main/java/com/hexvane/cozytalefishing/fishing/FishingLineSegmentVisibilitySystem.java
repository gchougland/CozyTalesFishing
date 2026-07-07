package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Hides third-person string segments from the line owner and first-person segments from all other
 * viewers so each client sees a chain anchored at the correct rod tip for their role.
 */
public final class FishingLineSegmentVisibilitySystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final ComponentType<EntityStore, EntityTrackerSystems.EntityViewer> entityViewerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public FishingLineSegmentVisibilitySystem() {
        this.entityViewerComponentType = EntityTrackerSystems.EntityViewer.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(entityViewerComponentType, uuidComponentType);
        this.dependencies =
            Collections.singleton(new SystemDependency<>(Order.AFTER, EntityTrackerSystems.CollectVisible.class));
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return EntityTrackerSystems.FIND_VISIBLE_ENTITIES_GROUP;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return EntityTickingSystem.maybeUseParallel(archetypeChunkSize, taskCount);
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        EntityTrackerSystems.EntityViewer entityViewer = archetypeChunk.getComponent(index, entityViewerComponentType);
        UUIDComponent viewerUuid = archetypeChunk.getComponent(index, uuidComponentType);
        if (entityViewer == null || viewerUuid == null) {
            return;
        }

        UUID viewerId = viewerUuid.getUuid();
        for (Iterator<Ref<EntityStore>> iterator = entityViewer.visible.iterator(); iterator.hasNext(); ) {
            Ref<EntityStore> targetRef = iterator.next();
            if (!targetRef.isValid()) {
                continue;
            }

            FishingLineSegmentComponent segment =
                commandBuffer.getComponent(targetRef, FishingLineSegmentComponent.getComponentType());
            if (segment == null) {
                continue;
            }

            UUID ownerId = segment.getLineOwnerUuid();
            LineSegmentAudience audience = segment.getAudience();
            if (audience == LineSegmentAudience.THIRD_PERSON_WORLD && ownerId.equals(viewerId)) {
                entityViewer.hiddenCount++;
                iterator.remove();
            } else if (audience == LineSegmentAudience.FIRST_PERSON_OWNER && !ownerId.equals(viewerId)) {
                entityViewer.hiddenCount++;
                iterator.remove();
            }
        }
    }
}
