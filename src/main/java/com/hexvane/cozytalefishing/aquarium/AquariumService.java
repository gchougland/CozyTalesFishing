package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumDisplayOffset;
import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hexvane.cozytalefishing.fish.FishItemStackFactory;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Shared aquarium block mutations outside tick systems. */
public final class AquariumService {
    private AquariumService() {}

    public static void ensureWater(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize size,
        int rotationIndex
    ) {
        Vector3i origin = new Vector3i(originBlock);
        world.execute(() -> AquariumFluidHelper.setWaterInFootprint(world, origin, size, rotationIndex));
    }

    public static void reconcileWater(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize size,
        int rotationIndex
    ) {
        Vector3i origin = new Vector3i(originBlock);
        world.execute(() -> AquariumFluidHelper.reconcileWaterInFootprint(world, origin, size, rotationIndex));
    }

    public static void ensureWater(@Nonnull World world, @Nonnull Vector3i originBlock) {
        world.execute(() -> AquariumFluidHelper.setWaterInFootprint(world, originBlock));
    }

    public static void clearWater(@Nonnull World world, @Nonnull Vector3i originBlock, @Nullable AquariumSize sizeFallback) {
        int rotationIndex = AquariumBlockHelper.rotationIndexAt(world, originBlock);
        AquariumSize size = sizeFallback != null ? sizeFallback : AquariumSize.Small;
        Vector3i origin = new Vector3i(originBlock);
        world.execute(() -> AquariumFluidHelper.clearWaterInFootprint(world, origin, size, rotationIndex));
    }

    public static void clearWater(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize sizeFallback,
        int rotationIndex
    ) {
        Vector3i origin = new Vector3i(originBlock);
        world.execute(() -> AquariumFluidHelper.clearWaterInFootprint(world, origin, sizeFallback, rotationIndex));
    }

    public static void clearWater(@Nonnull World world, @Nonnull Vector3i originBlock) {
        world.execute(() -> AquariumFluidHelper.clearWaterInFootprint(world, originBlock));
    }

    public static void ensureDisplay(
        @Nonnull World world,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize
    ) {
        AquariumBlock aquarium = chunkStore.getComponent(blockRef, AquariumBlock.getComponentType());
        if (aquarium == null || !aquarium.hasFish()) {
            return;
        }

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        if (hasLiveDisplay(entityStore, aquarium.getDisplayReference())) {
            aquarium.refreshDisplayLostTimeout();
            return;
        }

        aquarium.setDisplayReference(null);

        String fishItemId = aquarium.getFishItemId();
        if (fishItemId == null || fishItemId.isBlank()) {
            return;
        }

        Vector3d position = displayPositionForFish(world, originBlock, aquariumSize, fishItemId);
        float yaw = AquariumBlockHelper.displayYawFromOriginBlock(world, originBlock);
        var species = FishSpeciesRegistry.getSpeciesByItemId(fishItemId);
        float scaleModifier = species != null ? species.getAquariumDisplayScale() : 1.0f;
        boolean useIdleAnimation = species != null && species.isAquariumUseIdleAnimation();
        AquariumFishDisplaySpawner.spawnAndLink(
            entityStore,
            aquarium,
            fishItemId,
            position,
            yaw,
            originBlock,
            scaleModifier,
            useIdleAnimation
        );
        chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);
    }

    public static void ensureDecorationDisplays(
        @Nonnull World world,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize
    ) {
        AquariumBlock aquarium = chunkStore.getComponent(blockRef, AquariumBlock.getComponentType());
        if (aquarium == null || !aquarium.hasDecorations()) {
            return;
        }

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Vector3i origin = new Vector3i(originBlock);
        AquariumDecorationDisplaySpawner.despawnDisplaysAtOrigin(entityStore, origin);
        for (int slot = 0; slot < aquarium.getDecorationCount(); slot++) {
            aquarium.setDecorationDisplayRef(slot, null);
        }

        AquariumLayoutAsset layout = AquariumLayoutRegistry.getLayout(aquariumSize);
        float baseYaw = AquariumBlockHelper.displayYawFromOriginBlock(world, originBlock);
        boolean changed = false;

        for (int slot = 0; slot < aquarium.getDecorationCount(); slot++) {
            String itemId = aquarium.getDecorationItemIds().get(slot);
            AquariumDecorationAsset decorationAsset = AquariumDecorationRegistry.getByItemId(itemId);
            if (decorationAsset == null) {
                continue;
            }

            PersistentRef displayRef = aquarium.getDecorationDisplayRef(slot);
            if (hasLiveDecorationDisplay(entityStore, displayRef)) {
                aquarium.refreshDecorationDisplayLostTimeout();
                continue;
            }

            aquarium.setDecorationDisplayRef(slot, null);
            Vector3d position = displayPositionForDecoration(world, originBlock, aquariumSize, slot, decorationAsset);
            float yaw = baseYaw + decorationAsset.getDisplayYawOffset();
            AquariumDecorationDisplaySpawner.spawnAndLink(
                entityStore,
                aquarium,
                itemId,
                decorationAsset,
                position,
                yaw,
                originBlock,
                slot
            );
            changed = true;
        }

        if (changed) {
            chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);
        }
    }

    public static void spawnDecorationAtSlot(
        @Nonnull World world,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize,
        @Nonnull AquariumBlock aquarium,
        int slotIndex
    ) {
        if (slotIndex < 0 || slotIndex >= aquarium.getDecorationCount()) {
            return;
        }

        String itemId = aquarium.getDecorationItemIds().get(slotIndex);
        AquariumDecorationAsset decorationAsset = AquariumDecorationRegistry.getByItemId(itemId);
        if (decorationAsset == null) {
            return;
        }

        world.execute(
            () -> {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();
                AquariumBlock liveAquarium = chunkStore.getComponent(blockRef, AquariumBlock.getComponentType());
                if (liveAquarium == null || slotIndex >= liveAquarium.getDecorationCount()) {
                    return;
                }

                Vector3d position =
                    displayPositionForDecoration(world, originBlock, aquariumSize, slotIndex, decorationAsset);
                float yaw =
                    AquariumBlockHelper.displayYawFromOriginBlock(world, originBlock)
                        + decorationAsset.getDisplayYawOffset();
                AquariumDecorationDisplaySpawner.spawnAndLink(
                    entityStore,
                    liveAquarium,
                    itemId,
                    decorationAsset,
                    position,
                    yaw,
                    originBlock,
                    slotIndex
                );
                chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), liveAquarium);
            }
        );
    }

    public static boolean hasLiveDisplay(
        @Nonnull Store<EntityStore> entityStore,
        @Nullable PersistentRef displayReference
    ) {
        if (displayReference == null || !displayReference.isValid()) {
            return false;
        }

        Ref<EntityStore> entityRef = displayReference.getEntity(entityStore);
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        return entityStore.getComponent(entityRef, AquariumFishDisplayComponent.getComponentType()) != null;
    }

    public static boolean hasLiveDecorationDisplay(
        @Nonnull Store<EntityStore> entityStore,
        @Nullable PersistentRef displayReference
    ) {
        if (displayReference == null || !displayReference.isValid()) {
            return false;
        }

        Ref<EntityStore> entityRef = displayReference.getEntity(entityStore);
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        return entityStore.getComponent(entityRef, AquariumDecorationDisplayComponent.getComponentType()) != null;
    }

    public static void despawnDisplay(@Nonnull World world, @Nonnull AquariumBlock aquarium) {
        world.execute(() -> AquariumFishDisplaySpawner.despawnDisplay(world.getEntityStore().getStore(), aquarium));
    }

    public static void despawnAllDecorations(@Nonnull World world, @Nonnull AquariumBlock aquarium) {
        world.execute(() -> AquariumDecorationDisplaySpawner.despawnAll(world.getEntityStore().getStore(), aquarium));
    }

    public static void dropStoredFish(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize,
        @Nullable String fishItemId
    ) {
        if (fishItemId == null || fishItemId.isBlank()) {
            return;
        }
        world.execute(
            () -> {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();
                Vector3d dropPosition = displayPositionForFish(world, originBlock, aquariumSize, fishItemId);
                var drops =
                    ItemComponent.generateItemDrops(
                        entityStore,
                        List.of(FishItemStackFactory.forItemId(fishItemId)),
                        dropPosition,
                        Rotation3f.IDENTITY
                    );
                if (drops.length > 0) {
                    entityStore.addEntities(drops, AddReason.SPAWN);
                }
            }
        );
    }

    public static void dropStoredDecorations(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize,
        @Nonnull List<String> decorationItemIds
    ) {
        if (decorationItemIds.isEmpty()) {
            return;
        }
        world.execute(
            () -> {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();
                Vector3d dropPosition = AquariumBlockHelper.displayPosition(world, originBlock, aquariumSize);
                List<ItemStack> stacks = new ArrayList<>(decorationItemIds.size());
                for (String itemId : decorationItemIds) {
                    if (itemId != null && !itemId.isBlank()) {
                        stacks.add(new ItemStack(itemId, 1));
                    }
                }
                if (stacks.isEmpty()) {
                    return;
                }
                var drops =
                    ItemComponent.generateItemDrops(entityStore, stacks, dropPosition, Rotation3f.IDENTITY);
                if (drops.length > 0) {
                    entityStore.addEntities(drops, AddReason.SPAWN);
                }
            }
        );
    }

    @Nonnull
    public static Vector3d displayPositionForFish(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize,
        @Nullable String fishItemId
    ) {
        Vector3d base = AquariumBlockHelper.displayPosition(world, originBlock, aquariumSize);
        int rotationIndex = AquariumBlockHelper.rotationIndexAt(world, originBlock);
        if (fishItemId == null || fishItemId.isBlank()) {
            return base;
        }
        var species = FishSpeciesRegistry.getSpeciesByItemId(fishItemId);
        if (species == null) {
            return base;
        }
        return AquariumDisplayOffset.apply(base, species.getAquariumDisplayOffset(), rotationIndex);
    }

    @Nonnull
    public static Vector3d displayPositionForDecoration(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize aquariumSize,
        int slotIndex,
        @Nonnull AquariumDecorationAsset decorationAsset
    ) {
        Vector3d base = AquariumBlockHelper.displayPosition(world, originBlock, aquariumSize);
        int rotationIndex = AquariumBlockHelper.rotationIndexAt(world, originBlock);
        AquariumLayoutAsset layout = AquariumLayoutRegistry.getLayout(aquariumSize);
        Vector3d withSpot = AquariumDisplayOffset.apply(base, layout.getSpotOffset(slotIndex), rotationIndex);
        return AquariumDisplayOffset.apply(withSpot, decorationAsset.getDisplayOffset(), rotationIndex);
    }

    @Nonnull
    public static Vector3i worldOriginFromBlockInfo(
        @Nonnull BlockModule.BlockStateInfo info,
        @Nonnull com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk blockChunk
    ) {
        int localX = ChunkUtil.xFromBlockInColumn(info.getIndex());
        int localY = ChunkUtil.yFromBlockInColumn(info.getIndex());
        int localZ = ChunkUtil.zFromBlockInColumn(info.getIndex());
        return new Vector3i(
            ChunkUtil.worldCoordFromLocalCoord(blockChunk.getX(), localX),
            localY,
            ChunkUtil.worldCoordFromLocalCoord(blockChunk.getZ(), localZ)
        );
    }
}
