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
        var info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (aquarium == null || info == null || !info.getChunkRef().isValid()) {
            return;
        }

        var blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());
        if (blockChunk == null) {
            return;
        }

        Vector3i origin = AquariumService.worldOriginFromBlockInfo(info, blockChunk);
        var world = store.getExternalData().getWorld();
        AquariumSize aquariumSize = aquarium.getAquariumSize();
        if (aquariumSize == null) {
            aquariumSize = AquariumBlockHelper.aquariumSizeAt(world, origin);
        }
        if (aquariumSize == null) {
            aquariumSize = AquariumSize.Small;
        }

        int rotationIndex = AquariumBlockHelper.resolveRotationIndex(aquarium, world, origin);
        String fishItemId = aquarium.getFishItemId();
        var decorationItemIds = new java.util.ArrayList<>(aquarium.getDecorationItemIds());
        AquariumService.despawnDisplay(world, aquarium);
        AquariumService.despawnAllDecorations(world, aquarium);
        AquariumService.clearWater(world, origin, aquariumSize, rotationIndex);
        AquariumService.dropStoredFish(world, origin, aquariumSize, fishItemId);
        AquariumService.dropStoredDecorations(world, origin, aquariumSize, decorationItemIds);
    }
}
