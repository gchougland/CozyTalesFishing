package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

public final class AquariumBreakSystem extends RefSystem<ChunkStore> {
    private final Query<ChunkStore> query;

    public AquariumBreakSystem() {
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
    ) {}

    @Override
    public void onEntityRemove(
        @Nonnull Ref<ChunkStore> ref,
        @Nonnull RemoveReason reason,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        if (reason == RemoveReason.UNLOAD) {
            return;
        }

        var aquarium = commandBuffer.getComponent(ref, AquariumBlock.getComponentType());
        if (aquarium == null) {
            return;
        }

        var world = store.getExternalData().getWorld();
        String fishItemId = aquarium.getFishItemId();
        List<String> decorationItemIds = new ArrayList<>(aquarium.getDecorationItemIds());
        AquariumSize aquariumSize = aquarium.getAquariumSize();
        if (aquariumSize == null) {
            aquariumSize = AquariumSize.Small;
        }
        int rotationIndex = aquarium.getRotationIndex() >= 0 ? aquarium.getRotationIndex() : 0;

        Vector3i origin = null;
        var info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info != null && info.getChunkRef().isValid()) {
            var blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());
            if (blockChunk != null) {
                origin = AquariumService.worldOriginFromBlockInfo(info, blockChunk);
                if (aquarium.getAquariumSize() == null) {
                    AquariumSize atBlock = AquariumBlockHelper.aquariumSizeAt(world, origin);
                    if (atBlock != null) {
                        aquariumSize = atBlock;
                    }
                }
                if (aquarium.getRotationIndex() < 0) {
                    rotationIndex = AquariumBlockHelper.rotationIndexAt(world, origin);
                }
            }
        }

        AquariumService.despawnDisplay(world, aquarium);
        AquariumService.despawnAllDecorations(world, aquarium);

        if (origin != null) {
            Vector3i originCopy = new Vector3i(origin);
            AquariumSize sizeCopy = aquariumSize;
            int rotCopy = rotationIndex;
            world.execute(
                () -> {
                    Store<EntityStore> entityStore = world.getEntityStore().getStore();
                    // Match by BlockOrigin and by transform near the tank — prefab copies keep a stale origin.
                    AquariumFishDisplaySpawner.despawnDisplaysInVolume(entityStore, originCopy);
                    AquariumDecorationDisplaySpawner.despawnDisplaysInVolume(
                        entityStore,
                        originCopy,
                        sizeCopy,
                        rotCopy
                    );
                    AquariumFluidHelper.clearWaterInFootprint(world, originCopy, sizeCopy, rotCopy);
                    AquariumFluidHelper.clearWaterNearOrigin(world, originCopy, sizeCopy);
                }
            );
            AquariumPendingDrops.schedule(world, origin, aquariumSize, fishItemId, decorationItemIds);
        }
    }
}
