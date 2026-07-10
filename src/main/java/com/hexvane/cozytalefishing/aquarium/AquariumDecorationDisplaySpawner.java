package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
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

/** Spawns and removes intangible decoration display props inside aquariums. */
public final class AquariumDecorationDisplaySpawner {
    private static final float BLOCK_ENTITY_BASE_SCALE = 1.0f;

    private AquariumDecorationDisplaySpawner() {}

    @Nullable
    public static Ref<EntityStore> spawnDisplay(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull String decorationItemId,
        @Nonnull AquariumDecorationAsset decorationAsset,
        @Nonnull Vector3d position,
        float yawRadians,
        @Nonnull Vector3i blockOrigin,
        int slotIndex
    ) {
        Item item = Item.getAssetMap().getAsset(decorationItemId);
        if (item == null) {
            return null;
        }

        float scale = decorationAsset.getDisplayScale();
        Rotation3f rotation = new Rotation3f(0.0f, yawRadians, 0.0f);
        var holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(entityStore.getExternalData().takeNextNetworkId()));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(new Vector3d(position), rotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.addComponent(
            AquariumDecorationDisplayComponent.getComponentType(),
            new AquariumDecorationDisplayComponent(blockOrigin, slotIndex)
        );
        holder.ensureComponent(UUIDComponent.getComponentType());

        ModelAsset modelAsset = AquariumDisplayModelResolver.resolveModelAsset(item);
        if (modelAsset != null) {
            Model model = Model.createScaledModel(modelAsset, Math.max(0.01f, item.getScale() * scale));
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
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        } else {
            String blockTypeKey = AquariumDisplayModelResolver.blockTypeKey(item);
            if (blockTypeKey == null) {
                return null;
            }
            holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(blockTypeKey));
            holder.addComponent(
                EntityScaleComponent.getComponentType(),
                new EntityScaleComponent(Math.max(0.01f, scale * BLOCK_ENTITY_BASE_SCALE))
            );
        }

        return entityStore.addEntity(holder, AddReason.SPAWN);
    }

    @Nullable
    public static PersistentRef spawnAndLink(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull AquariumBlock aquarium,
        @Nonnull String decorationItemId,
        @Nonnull AquariumDecorationAsset decorationAsset,
        @Nonnull Vector3d position,
        float yawRadians,
        @Nonnull Vector3i blockOrigin,
        int slotIndex
    ) {
        Ref<EntityStore> displayRef =
            spawnDisplay(entityStore, decorationItemId, decorationAsset, position, yawRadians, blockOrigin, slotIndex);
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
        aquarium.setDecorationDisplayRef(slotIndex, persistentRef);
        return persistentRef;
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

    public static void despawnAll(@Nonnull Store<EntityStore> entityStore, @Nonnull AquariumBlock aquarium) {
        for (PersistentRef ref : aquarium.getDecorationDisplayRefs()) {
            despawnDisplayByRef(entityStore, ref);
        }
        aquarium.getDecorationDisplayRefs().clear();
    }

    /** Removes every decoration display prop tied to an aquarium origin (including orphaned saves). */
    public static void despawnDisplaysAtOrigin(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull Vector3i blockOrigin
    ) {
        java.util.Set<Ref<EntityStore>> refs = new java.util.HashSet<>();
        entityStore.forEachEntityParallel(
            AquariumDecorationDisplayComponent.getComponentType(),
            (index, chunk, ignored) -> {
                AquariumDecorationDisplayComponent component =
                    chunk.getComponent(index, AquariumDecorationDisplayComponent.getComponentType());
                if (component == null || component.getBlockOrigin() == null) {
                    return;
                }
                if (component.getBlockOrigin().x != blockOrigin.x
                    || component.getBlockOrigin().y != blockOrigin.y
                    || component.getBlockOrigin().z != blockOrigin.z) {
                    return;
                }
                synchronized (refs) {
                    refs.add(chunk.getReferenceTo(index));
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
