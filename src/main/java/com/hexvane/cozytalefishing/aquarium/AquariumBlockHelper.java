package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Resolves aquarium block origins, centers, and rotations. */
public final class AquariumBlockHelper {
    private AquariumBlockHelper() {}

    @Nonnull
    public static Vector3i resolveOriginBlock(@Nonnull World world, @Nonnull Vector3i blockPos) {
        int filler = fillerAt(world, blockPos.x, blockPos.y, blockPos.z);
        int fillerX = FillerBlockUtil.unpackX(filler);
        int fillerY = FillerBlockUtil.unpackY(filler);
        int fillerZ = FillerBlockUtil.unpackZ(filler);
        if (fillerX == 0 && fillerY == 0 && fillerZ == 0) {
            return new Vector3i(blockPos);
        }
        return new Vector3i(blockPos.x - fillerX, blockPos.y - fillerY, blockPos.z - fillerZ);
    }

    @Nullable
    public static AquariumSize aquariumSizeAt(@Nonnull World world, @Nonnull Vector3i originBlock) {
        BlockType blockType = world.getBlockType(originBlock.x, originBlock.y, originBlock.z);
        if (blockType == null) {
            return null;
        }
        return AquariumConstants.sizeForBlockId(blockType.getId());
    }

    public static float yawFromOriginBlock(@Nonnull World world, @Nonnull Vector3i originBlock) {
        return (float) RotationTuple.get(rotationIndexAt(world, originBlock)).yaw().getRadians();
    }

    /** Block yaw adjusted so fish item models face along the aquarium length. */
    public static float displayYawFromOriginBlock(@Nonnull World world, @Nonnull Vector3i originBlock) {
        return yawFromOriginBlock(world, originBlock) + (float) (Math.PI * 0.5);
    }

    public static int rotationIndexAt(@Nonnull World world, @Nonnull Vector3i originBlock) {
        return rotationIndexAtBlock(world, originBlock.x, originBlock.y, originBlock.z);
    }

    public static int resolveRotationIndex(
        @Nonnull AquariumBlock aquarium,
        @Nonnull World world,
        @Nonnull Vector3i originBlock
    ) {
        int stored = aquarium.getRotationIndex();
        if (stored >= 0) {
            return stored;
        }
        return rotationIndexAt(world, originBlock);
    }

    @Nonnull
    public static Vector3d displayPosition(@Nonnull World world, @Nonnull Vector3i originBlock, @Nonnull AquariumSize size) {
        if (world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(originBlock.x, originBlock.z)) == null) {
            return new Vector3d(originBlock.x + 0.5, originBlock.y + 0.45, originBlock.z + 0.5);
        }

        BlockType blockType = world.getBlockType(originBlock.x, originBlock.y, originBlock.z);
        if (blockType == null) {
            return new Vector3d(originBlock.x + 0.5, originBlock.y + 0.45, originBlock.z + 0.5);
        }

        int rotationIndex = rotationIndexAtBlock(world, originBlock.x, originBlock.y, originBlock.z);
        var hitbox = BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex());
        if (hitbox == null) {
            return centerForSize(originBlock, size);
        }

        var rotated = hitbox.get(rotationIndex);
        var box = rotated.getBoundingBox();
        double centerX = originBlock.x + (box.min.x + box.max.x) * 0.5;
        double centerY = originBlock.y + (box.min.y + box.max.y) * 0.5;
        double centerZ = originBlock.z + (box.min.z + box.max.z) * 0.5;
        return new Vector3d(centerX, centerY, centerZ);
    }

    @Nonnull
    private static Vector3d centerForSize(@Nonnull Vector3i originBlock, @Nonnull AquariumSize size) {
        return switch (size) {
            case Small -> new Vector3d(originBlock.x + 0.5, originBlock.y + 0.45, originBlock.z + 0.5);
            case Wide2x1 -> new Vector3d(originBlock.x + 1.0, originBlock.y + 0.45, originBlock.z + 0.5);
            case Tall3x2x2 -> new Vector3d(originBlock.x + 0.5, originBlock.y + 1.0, originBlock.z + 1.0);
        };
    }

    public static void forEachFootprintBlock(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull FootprintConsumer consumer
    ) {
        forEachFootprintBlock(world, originBlock, null, consumer);
    }

    public static void forEachFootprintBlock(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nullable AquariumSize sizeFallback,
        @Nonnull FootprintConsumer consumer
    ) {
        BlockType blockType = world.getBlockType(originBlock.x, originBlock.y, originBlock.z);
        if (blockType != null) {
            forEachFootprintForBlockType(world, originBlock, blockType, consumer);
            return;
        }

        if (sizeFallback != null) {
            forEachFootprintBySize(world, originBlock, sizeFallback, consumer);
            return;
        }

        consumer.accept(originBlock.x, originBlock.y, originBlock.z);
    }

    public static void forEachFootprintBySize(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize size,
        @Nonnull FootprintConsumer consumer
    ) {
        forEachFootprintBySize(world, originBlock, size, rotationIndexAt(world, originBlock), consumer);
    }

    public static void forEachFootprintBySize(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull AquariumSize size,
        int rotationIndex,
        @Nonnull FootprintConsumer consumer
    ) {
        BlockType blockType = BlockType.getAssetMap().getAsset(AquariumConstants.blockIdForSize(size));
        if (blockType == null) {
            consumer.accept(originBlock.x, originBlock.y, originBlock.z);
            return;
        }
        forEachFootprintForBlockType(world, originBlock, blockType, rotationIndex, consumer);
    }

    private static void forEachFootprintForBlockType(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull BlockType blockType,
        @Nonnull FootprintConsumer consumer
    ) {
        forEachFootprintForBlockType(
            world,
            originBlock,
            blockType,
            rotationIndexAt(world, originBlock),
            consumer
        );
    }

    private static void forEachFootprintForBlockType(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull BlockType blockType,
        int rotationIndex,
        @Nonnull FootprintConsumer consumer
    ) {
        var hitbox = BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex());
        if (hitbox == null) {
            consumer.accept(originBlock.x, originBlock.y, originBlock.z);
            return;
        }

        FillerBlockUtil.forEachFillerBlock(
            hitbox.get(rotationIndex),
            (x, y, z) -> consumer.accept(originBlock.x + x, originBlock.y + y, originBlock.z + z)
        );
    }

    @Nullable
    public static Ref<ChunkStore> blockEntityAtOrigin(@Nonnull World world, @Nonnull Vector3i originBlock) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(originBlock.x, originBlock.z));
        if (chunk == null) {
            return null;
        }
        var blockRef = chunk.getBlockComponentEntity(originBlock.x, originBlock.y, originBlock.z);
        if (blockRef != null && blockRef.isValid()) {
            return blockRef;
        }
        return ensureBlockEntityAt(chunk, originBlock.x, originBlock.y, originBlock.z);
    }

    private static int rotationIndexAtBlock(@Nonnull World world, int x, int y, int z) {
        if (y < 0 || y >= ChunkUtil.HEIGHT) {
            return RotationTuple.NONE_INDEX;
        }
        BlockSection blockSection = blockSectionAtBlock(world, x, y, z);
        if (blockSection == null) {
            return RotationTuple.NONE_INDEX;
        }
        return blockSection.getRotationIndex(x, y, z);
    }

    private static int fillerAt(@Nonnull World world, int x, int y, int z) {
        if (y < 0 || y >= ChunkUtil.HEIGHT) {
            return FillerBlockUtil.NO_FILLER;
        }
        BlockSection blockSection = blockSectionAtBlock(world, x, y, z);
        if (blockSection == null) {
            return FillerBlockUtil.NO_FILLER;
        }
        return blockSection.getFiller(x, y, z);
    }

    @Nullable
    private static BlockSection blockSectionAtBlock(@Nonnull World world, int x, int y, int z) {
        var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(sectionRef, BlockSection.getComponentType());
    }

    @Nullable
    private static Ref<ChunkStore> ensureBlockEntityAt(@Nonnull WorldChunk chunk, int x, int y, int z) {
        var blockRef = chunk.getBlockComponentEntity(x, y, z);
        if (blockRef != null) {
            return blockRef;
        }

        if (fillerAt(chunk.getWorld(), x, y, z) != FillerBlockUtil.NO_FILLER) {
            return null;
        }

        BlockType blockType = chunk.getBlockType(x, y, z);
        if (blockType == null || blockType.getBlockEntity() == null) {
            return null;
        }

        var data = blockType.getBlockEntity().clone();
        data.putComponent(
            BlockModule.BlockStateInfo.getComponentType(),
            new BlockModule.BlockStateInfo(ChunkUtil.indexBlockInColumn(x, y, z), chunk.getReference())
        );
        return chunk.getWorld().getChunkStore().getStore().addEntity(data, AddReason.SPAWN);
    }

    @FunctionalInterface
    public interface FootprintConsumer {
        void accept(int x, int y, int z);
    }
}
