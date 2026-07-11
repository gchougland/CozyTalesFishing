package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.FishShadowAnimations;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Spawns and removes intangible fish display props inside aquariums. */
public final class AquariumFishDisplaySpawner {
    private AquariumFishDisplaySpawner() {}

    @Nullable
    public static Ref<EntityStore> spawnDisplay(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull String fishItemId,
        @Nonnull Vector3d position,
        float yawRadians,
        @Nonnull Vector3i blockOrigin,
        float scaleModifier,
        boolean useIdleAnimation
    ) {
        Item item = Item.getAssetMap().getAsset(fishItemId);
        if (item == null) {
            return null;
        }

        ModelAsset modelAsset = AquariumDisplayModelResolver.resolveModelAsset(item);
        if (modelAsset == null) {
            return null;
        }

        float scale = Math.max(0.01f, item.getScale() * scaleModifier);
        Model model = Model.createScaledModel(modelAsset, scale);
        if (item.getTexture() != null && !item.getTexture().equals(model.getTexture())) {
            model =
                new Model(
                    model.getModelAssetId(),
                    model.getScale(),
                    model.getRandomAttachmentIds(),
                    model.getAttachments(),
                    model.getBoundingBox(),
                    model.getModel(),
                    item.getTexture(),
                    model.getGradientSet(),
                    model.getGradientId(),
                    model.getEyeHeight(),
                    model.getCrouchOffset(),
                    model.getSittingOffset(),
                    model.getSleepingOffset(),
                    model.getAnimationSetMap(),
                    model.getCamera(),
                    model.getLight(),
                    model.getParticles(),
                    model.getTrails(),
                    model.getPhysicsValues(),
                    model.getDetailBoxes(),
                    model.getPhobia(),
                    model.getPhobiaModelAssetId()
                );
        }

        Rotation3f rotation = new Rotation3f(0.0f, yawRadians, 0.0f);
        var holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(entityStore.getExternalData().takeNextNetworkId()));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(new Vector3d(position), rotation));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.addComponent(AquariumFishDisplayComponent.getComponentType(), new AquariumFishDisplayComponent(blockOrigin));
        holder.ensureComponent(UUIDComponent.getComponentType());
        FishShadowAnimations.prepareMovementAnimation(holder, model, useIdleAnimation);

        Ref<EntityStore> displayRef = entityStore.addEntity(holder, AddReason.SPAWN);
        if (displayRef != null) {
            FishShadowAnimations.playMovementAnimation(displayRef, entityStore, useIdleAnimation);
        }
        return displayRef;
    }

    @Nullable
    public static PersistentRef spawnAndLink(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull AquariumBlock aquarium,
        @Nonnull String fishItemId,
        @Nonnull Vector3d position,
        float yawRadians,
        @Nonnull Vector3i blockOrigin,
        float scaleModifier,
        boolean useIdleAnimation
    ) {
        Ref<EntityStore> displayRef =
            spawnDisplay(entityStore, fishItemId, position, yawRadians, blockOrigin, scaleModifier, useIdleAnimation);
        if (displayRef == null) {
            return null;
        }
        UUIDComponent uuidComponent = entityStore.getComponent(displayRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            entityStore.removeEntity(displayRef, RemoveReason.REMOVE);
            return null;
        }
        var persistentRef = new PersistentRef();
        persistentRef.setEntity(displayRef, uuidComponent.getUuid());
        aquarium.setDisplayReference(persistentRef);
        return persistentRef;
    }

    public static void despawnDisplay(@Nonnull Store<EntityStore> entityStore, @Nonnull AquariumBlock aquarium) {
        despawnDisplayByRef(entityStore, aquarium.getDisplayReference());
        aquarium.setDisplayReference(null);
    }

    public static void despawnDisplayByRef(
        @Nonnull Store<EntityStore> entityStore,
        @Nullable PersistentRef displayReference
    ) {
        if (displayReference == null) {
            return;
        }
        Ref<EntityStore> displayRef = displayReference.getEntity(entityStore);
        if (displayRef != null && displayRef.isValid()) {
            entityStore.removeEntity(displayRef, RemoveReason.REMOVE);
        }
    }

    /** Removes every fish display prop tied to an aquarium origin (including orphaned prefab copies). */
    public static void despawnDisplaysAtOrigin(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull Vector3i blockOrigin
    ) {
        despawnDisplaysInVolume(entityStore, blockOrigin);
    }

    /** Prefab fish displays keep a stale BlockOrigin; also match by transform near the aquarium. */
    public static void despawnDisplaysInVolume(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull Vector3i blockOrigin
    ) {
        java.util.Set<Ref<EntityStore>> refs = new java.util.HashSet<>();
        entityStore.forEachEntityParallel(
            AquariumFishDisplayComponent.getComponentType(),
            (index, chunk, ignored) -> {
                AquariumFishDisplayComponent component =
                    chunk.getComponent(index, AquariumFishDisplayComponent.getComponentType());
                if (component == null) {
                    return;
                }
                Vector3i origin = component.getBlockOrigin();
                if (origin != null
                    && origin.x == blockOrigin.x
                    && origin.y == blockOrigin.y
                    && origin.z == blockOrigin.z) {
                    synchronized (refs) {
                        refs.add(chunk.getReferenceTo(index));
                    }
                    return;
                }
                TransformComponent transform =
                    chunk.getComponent(index, TransformComponent.getComponentType());
                if (transform == null) {
                    return;
                }
                Vector3d p = transform.getPosition();
                int bx = (int) Math.floor(p.x);
                int by = (int) Math.floor(p.y);
                int bz = (int) Math.floor(p.z);
                if (Math.abs(bx - blockOrigin.x) <= 3
                    && by >= blockOrigin.y - 1
                    && by <= blockOrigin.y + 3
                    && Math.abs(bz - blockOrigin.z) <= 3) {
                    synchronized (refs) {
                        refs.add(chunk.getReferenceTo(index));
                    }
                }
            }
        );
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) {
                entityStore.removeEntity(ref, RemoveReason.REMOVE);
            }
        }
    }
}
