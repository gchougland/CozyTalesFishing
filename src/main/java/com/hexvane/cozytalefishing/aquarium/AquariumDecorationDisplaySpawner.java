package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
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
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hexvane.cozytalefishing.fish.AquariumSize;
import java.util.HashSet;
import java.util.Set;
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
        despawnDisplaysInVolume(entityStore, blockOrigin, null, 0);
    }

    /**
     * Removes decoration displays whose {@code BlockOrigin} matches, or whose transform sits in/near the aquarium
     * volume. Prefab-pasted displays often keep a stale BlockOrigin from the export world.
     */
    public static void despawnDisplaysInVolume(
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull Vector3i blockOrigin,
        @Nullable AquariumSize size,
        int rotationIndex
    ) {
        Set<Long> volumeCells = volumeCells(blockOrigin, size, rotationIndex);
        Set<Ref<EntityStore>> refs = new HashSet<>();
        entityStore.forEachEntityParallel(
            AquariumDecorationDisplayComponent.getComponentType(),
            (index, chunk, ignored) -> {
                AquariumDecorationDisplayComponent component =
                    chunk.getComponent(index, AquariumDecorationDisplayComponent.getComponentType());
                if (component == null) {
                    return;
                }
                if (originMatches(component.getBlockOrigin(), blockOrigin)) {
                    synchronized (refs) {
                        refs.add(chunk.getReferenceTo(index));
                    }
                    return;
                }
                TransformComponent transform =
                    chunk.getComponent(index, TransformComponent.getComponentType());
                if (transform != null && inVolume(transform.getPosition(), blockOrigin, volumeCells)) {
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

    private static boolean originMatches(@Nullable Vector3i origin, @Nonnull Vector3i expected) {
        return origin != null
            && origin.x == expected.x
            && origin.y == expected.y
            && origin.z == expected.z;
    }

    private static boolean inVolume(
        @Nonnull Vector3d position,
        @Nonnull Vector3i blockOrigin,
        @Nonnull Set<Long> volumeCells
    ) {
        int bx = (int) Math.floor(position.x);
        int by = (int) Math.floor(position.y);
        int bz = (int) Math.floor(position.z);
        if (volumeCells.contains(packCell(bx, by, bz))) {
            return true;
        }
        return Math.abs(bx - blockOrigin.x) <= 3
            && by >= blockOrigin.y - 1
            && by <= blockOrigin.y + 3
            && Math.abs(bz - blockOrigin.z) <= 3;
    }

    @Nonnull
    private static Set<Long> volumeCells(
        @Nonnull Vector3i blockOrigin,
        @Nullable AquariumSize size,
        int rotationIndex
    ) {
        Set<Long> cells = new HashSet<>();
        if (size == null) {
            cells.add(packCell(blockOrigin.x, blockOrigin.y, blockOrigin.z));
            return cells;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(AquariumConstants.blockIdForSize(size));
        if (blockType == null) {
            cells.add(packCell(blockOrigin.x, blockOrigin.y, blockOrigin.z));
            return cells;
        }
        var hitbox = BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex());
        if (hitbox == null) {
            cells.add(packCell(blockOrigin.x, blockOrigin.y, blockOrigin.z));
            return cells;
        }
        FillerBlockUtil.forEachFillerBlock(
            hitbox.get(rotationIndex),
            (x, y, z) -> {
                int wx = blockOrigin.x + x;
                int wy = blockOrigin.y + y;
                int wz = blockOrigin.z + z;
                cells.add(packCell(wx, wy, wz));
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            cells.add(packCell(wx + dx, wy + dy, wz + dz));
                        }
                    }
                }
            }
        );
        return cells;
    }

    private static long packCell(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | (long) z & 0x3FFFFFFL;
    }
}
