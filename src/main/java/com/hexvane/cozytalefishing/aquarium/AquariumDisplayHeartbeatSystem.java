package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/**
 * Keeps aquarium fish and decoration display entities in sync with their block state.
 *
 * <p>Plot teardown can clear aquarium voxels without removing the chunk-store block entity. This system must not
 * restore water/fish for those orphans — that is exactly the "water stays, fish comes back a second later" bug.
 */
public final class AquariumDisplayHeartbeatSystem extends EntityTickingSystem<ChunkStore> {
    private final Query<ChunkStore> query;

    public AquariumDisplayHeartbeatSystem() {
        query = Query.and(AquariumBlock.getComponentType(), BlockModule.BlockStateInfo.getComponentType());
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        AquariumBlock aquarium = archetypeChunk.getComponent(index, AquariumBlock.getComponentType());
        BlockModule.BlockStateInfo info = archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
        if (aquarium == null || info == null || !info.getChunkRef().isValid()) {
            return;
        }

        Ref<ChunkStore> blockRef = archetypeChunk.getReferenceTo(index);
        var world = store.getExternalData().getWorld();
        Store<EntityStore> entityStore = world.getEntityStore().getStore();

        var blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());
        if (blockChunk == null) {
            return;
        }

        Vector3i origin = AquariumService.worldOriginFromBlockInfo(info, blockChunk);

        // Demolish left the AquariumBlock entity alive with no voxel → do not restore; remove so BreakSystem cleans up.
        if (!isLiveAquariumBlock(world, origin)) {
            commandBuffer.removeEntity(blockRef, RemoveReason.REMOVE);
            return;
        }

        AquariumSize aquariumSize = aquarium.getAquariumSize();
        if (aquariumSize == null) {
            aquariumSize = AquariumBlockHelper.aquariumSizeAt(world, origin);
        }

        if (aquariumSize != null) {
            int rotationIndex =
                aquarium.getRotationIndex() >= 0
                    ? aquarium.getRotationIndex()
                    : AquariumBlockHelper.rotationIndexAt(world, origin);
            if (!AquariumFluidHelper.hasWaterInFootprint(world, origin, aquariumSize, rotationIndex)) {
                AquariumService.reconcileWater(world, origin, aquariumSize, rotationIndex);
            }
        }

        syncFishDisplay(dt, aquarium, blockRef, world, entityStore, store, origin, aquariumSize, commandBuffer);
        syncDecorationDisplays(dt, aquarium, blockRef, world, entityStore, store, origin, aquariumSize, commandBuffer);
    }

    /**
     * True when the world voxel at {@code origin} is still an aquarium. Uses in-memory chunk only (safe from tick).
     * If the chunk is unloaded, assume live so we do not scrub during normal unload.
     */
    private static boolean isLiveAquariumBlock(@Nonnull World world, @Nonnull Vector3i origin) {
        if (origin.y < 0 || origin.y >= ChunkUtil.HEIGHT) {
            return false;
        }
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(origin.x, origin.z));
        if (chunk == null) {
            return true;
        }
        BlockType blockType = chunk.getBlockType(origin.x, origin.y, origin.z);
        return blockType != null && AquariumConstants.isAquariumBlockId(blockType.getId());
    }

    private static void syncFishDisplay(
        float dt,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull com.hypixel.hytale.server.core.universe.world.World world,
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull Store<ChunkStore> store,
        @Nonnull Vector3i origin,
        @Nullable AquariumSize aquariumSize,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        if (!aquarium.hasFish()) {
            if (aquarium.getDisplayReference() != null) {
                PersistentRef displayRef = aquarium.getDisplayReference();
                aquarium.setDisplayReference(null);
                commandBuffer.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);
                world.execute(() -> AquariumFishDisplaySpawner.despawnDisplayByRef(entityStore, displayRef));
            }
            return;
        }

        if (aquariumSize == null) {
            return;
        }

        PersistentRef displayReference = aquarium.getDisplayReference();
        if (AquariumService.hasLiveDisplay(entityStore, displayReference)) {
            aquarium.refreshDisplayLostTimeout();
            return;
        }

        if (displayReference != null && displayReference.isValid() && !aquarium.tickDisplayLostTimeout(dt)) {
            return;
        }

        aquarium.setDisplayReference(null);
        aquarium.refreshDisplayLostTimeout();
        AquariumSize resolvedSize = aquariumSize;
        world.execute(
            () ->
                AquariumService.ensureDisplay(
                    world,
                    blockRef,
                    world.getChunkStore().getStore(),
                    origin,
                    resolvedSize
                )
        );
    }

    private static void syncDecorationDisplays(
        float dt,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull com.hypixel.hytale.server.core.universe.world.World world,
        @Nonnull Store<EntityStore> entityStore,
        @Nonnull Store<ChunkStore> store,
        @Nonnull Vector3i origin,
        @Nullable AquariumSize aquariumSize,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        trimOrphanDecorationRefs(aquarium);

        if (!aquarium.hasDecorations()) {
            if (!aquarium.getDecorationDisplayRefs().isEmpty()) {
                List<PersistentRef> refs = new ArrayList<>(aquarium.getDecorationDisplayRefs());
                aquarium.getDecorationDisplayRefs().clear();
                commandBuffer.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);
                world.execute(
                    () -> {
                        for (PersistentRef ref : refs) {
                            AquariumDecorationDisplaySpawner.despawnDisplayByRef(entityStore, ref);
                        }
                    }
                );
            }
            return;
        }

        if (aquariumSize == null) {
            return;
        }

        boolean needsRespawn = false;
        for (int slot = 0; slot < aquarium.getDecorationCount(); slot++) {
            PersistentRef displayRef = aquarium.getDecorationDisplayRef(slot);
            if (AquariumService.hasLiveDecorationDisplay(entityStore, displayRef)) {
                aquarium.refreshDecorationDisplayLostTimeout();
                continue;
            }

            if (displayRef != null && displayRef.isValid() && !aquarium.tickDecorationDisplayLostTimeout(dt)) {
                continue;
            }

            aquarium.setDecorationDisplayRef(slot, null);
            aquarium.refreshDecorationDisplayLostTimeout();
            needsRespawn = true;
        }

        if (needsRespawn) {
            commandBuffer.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);
            AquariumSize resolvedSize = aquariumSize;
            world.execute(
                () ->
                    AquariumService.ensureDecorationDisplays(
                        world,
                        blockRef,
                        world.getChunkStore().getStore(),
                        origin,
                        resolvedSize
                    )
            );
        }
    }

    private static void trimOrphanDecorationRefs(@Nonnull AquariumBlock aquarium) {
        while (aquarium.getDecorationDisplayRefs().size() > aquarium.getDecorationCount()) {
            aquarium.getDecorationDisplayRefs().remove(aquarium.getDecorationDisplayRefs().size() - 1);
        }
    }
}
