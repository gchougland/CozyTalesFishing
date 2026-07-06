package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Spawns fixed-scale string segment props (prop entities do not stretch reliably at runtime). */
public final class SegmentEntityPool {
    private static boolean loggedMissingModel;

    private SegmentEntityPool() {}

    @Nullable
    public static Ref<EntityStore> spawnSegment(
        @Nonnull Store<EntityStore> store,
        @Nonnull Vector3d position,
        @Nonnull Rotation3f rotation,
        int segmentIndex
    ) {
        return spawnSegmentHolder(store.getExternalData().takeNextNetworkId(), position, rotation, segmentIndex, holder ->
            store.addEntity(holder, AddReason.SPAWN)
        );
    }

    @Nullable
    public static Ref<EntityStore> spawnSegment(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d position,
        @Nonnull Rotation3f rotation,
        int segmentIndex
    ) {
        return spawnSegmentHolder(
            commandBuffer.getExternalData().takeNextNetworkId(),
            position,
            rotation,
            segmentIndex,
            holder -> commandBuffer.addEntity(holder, AddReason.SPAWN)
        );
    }

    @FunctionalInterface
    private interface SegmentSpawner {
        Ref<EntityStore> spawn(com.hypixel.hytale.component.Holder<EntityStore> holder);
    }

    @Nullable
    private static Ref<EntityStore> spawnSegmentHolder(
        int networkId,
        @Nonnull Vector3d position,
        @Nonnull Rotation3f rotation,
        int segmentIndex,
        @Nonnull SegmentSpawner spawner
    ) {
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(FishingConstants.STRING_SEGMENT_MODEL_ID);
        if (modelAsset == null) {
            logMissingModelOnce();
            return null;
        }

        Model model = Model.createUnitScaleModel(modelAsset);
        var holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(
            PersistentModel.getComponentType(),
            new PersistentModel(new Model.ModelReference(FishingConstants.STRING_SEGMENT_MODEL_ID, 1.0f, null, true))
        );
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.ensureComponent(UUIDComponent.getComponentType());
        Ref<EntityStore> segmentRef = spawner.spawn(holder);

        if (segmentIndex == 0 || segmentIndex == FishingConstants.SEGMENT_COUNT - 1) {
            FishingDebugLog.info(
                "String segment[%d] fixedScale=1.0 model=%s pos=(%.2f, %.2f, %.2f)",
                segmentIndex,
                modelAsset.getModel(),
                position.x,
                position.y,
                position.z
            );
        }
        return segmentRef;
    }

    public static void updateSegment(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> segmentRef,
        @Nonnull Vector3d position,
        @Nonnull Rotation3f rotation
    ) {
        if (!segmentRef.isValid()) {
            return;
        }
        TransformComponent transform = commandBuffer.getComponent(segmentRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(position);
            transform.setRotation(rotation);
        }
        HeadRotation headRotation = commandBuffer.getComponent(segmentRef, HeadRotation.getComponentType());
        if (headRotation != null) {
            headRotation.setRotation(rotation);
        }
    }

    public static void despawnAll(@Nonnull Store<EntityStore> store, @Nonnull FishingLineComponent line) {
        int removed = 0;
        for (Ref<EntityStore> segmentRef : line.getSegmentRefs()) {
            if (segmentRef != null && segmentRef.isValid()) {
                store.removeEntity(segmentRef, RemoveReason.REMOVE);
                removed++;
            }
        }
        if (removed > 0) {
            FishingDebugLog.info("Despawned %d string segments", removed);
        }
    }

    public static void despawnAll(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull FishingLineComponent line) {
        int removed = 0;
        for (Ref<EntityStore> segmentRef : line.getSegmentRefs()) {
            if (segmentRef != null) {
                commandBuffer.removeEntity(segmentRef, RemoveReason.REMOVE);
                removed++;
            }
        }
        if (removed > 0) {
            FishingDebugLog.info("Despawned %d string segments", removed);
        }
    }

    private static void logMissingModelOnce() {
        if (loggedMissingModel) {
            return;
        }
        loggedMissingModel = true;
        FishingDebugLog.warn("Missing string segment model asset %s", FishingConstants.STRING_SEGMENT_MODEL_ID);
    }
}
