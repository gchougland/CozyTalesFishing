package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

public final class AquariumPlaceSystem extends RefSystem<ChunkStore> {
    private final Query<ChunkStore> query;

    public AquariumPlaceSystem() {
        query = Query.and(AquariumBlock.getComponentType(), BlockModule.BlockStateInfo.getComponentType());
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }

    @Override
    public void onEntityAdded(
        @Nonnull Ref<ChunkStore> ref,
        @Nonnull AddReason reason,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        var info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null || !info.getChunkRef().isValid()) {
            return;
        }

        var blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());
        if (blockChunk == null) {
            return;
        }

        Vector3i origin = AquariumService.worldOriginFromBlockInfo(info, blockChunk);
        var world = store.getExternalData().getWorld();

        AquariumBlock aquarium = commandBuffer.getComponent(ref, AquariumBlock.getComponentType());
        if (aquarium != null) {
            int rotationIndex = AquariumBlockHelper.rotationIndexAt(world, origin);

            BlockType blockType = world.getBlockType(origin.x, origin.y, origin.z);
            AquariumSize size =
                blockType != null ? AquariumConstants.sizeForBlockId(blockType.getId()) : null;
            if (size == null) {
                size = aquarium.getAquariumSize();
            }

            boolean changed = false;
            if (size != null) {
                aquarium.setAquariumSize(size);
                changed = true;
            }
            if (aquarium.getRotationIndex() != rotationIndex) {
                aquarium.setRotationIndex(rotationIndex);
                changed = true;
            }
            if (changed) {
                commandBuffer.putComponent(ref, AquariumBlock.getComponentType(), aquarium);
            }

            if (size != null) {
                AquariumService.reconcileWater(world, origin, size, rotationIndex);
                if (reason == AddReason.LOAD) {
                    boolean changedOnLoad = false;
                    if (aquarium.hasFish()) {
                        aquarium.setDisplayReference(null);
                        changedOnLoad = true;
                    }
                    if (aquarium.hasDecorations()) {
                        aquarium.clearDecorationDisplayRefs();
                        changedOnLoad = true;
                    }
                    if (changedOnLoad) {
                        commandBuffer.putComponent(ref, AquariumBlock.getComponentType(), aquarium);
                    }
                    scheduleDisplayRestore(world, ref, origin, size);
                }
                return;
            }
        }

        AquariumService.ensureWater(world, origin);
    }

    private static void scheduleDisplayRestore(
        @Nonnull com.hypixel.hytale.server.core.universe.world.World world,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize size
    ) {
        Vector3i originCopy = new Vector3i(origin);
        world.execute(
            () -> {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();
                AquariumDecorationDisplaySpawner.despawnDisplaysAtOrigin(entityStore, originCopy);
                Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                AquariumBlock liveAquarium = chunkStore.getComponent(blockRef, AquariumBlock.getComponentType());
                if (liveAquarium != null && liveAquarium.hasDecorations()) {
                    liveAquarium.clearDecorationDisplayRefs();
                    chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), liveAquarium);
                }
                AquariumService.ensureDisplay(world, blockRef, chunkStore, originCopy, size);
                AquariumService.ensureDecorationDisplays(world, blockRef, chunkStore, originCopy, size);
            }
        );
    }

    @Override
    public void onEntityRemove(
        @Nonnull Ref<ChunkStore> ref,
        @Nonnull RemoveReason reason,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {}
}
