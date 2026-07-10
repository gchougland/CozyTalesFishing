package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Place/remove for parked fishing boat blocks above the water surface. */
public final class FishingBoatBlockHelper {
  private FishingBoatBlockHelper() {}

  public static boolean isPlacedBoatBlock(@Nonnull World world, @Nonnull Vector3i blockPos) {
    return isPlacedBoatBlockId(world.getBlockType(blockPos.x, blockPos.y, blockPos.z).getId());
  }

  public static boolean isPlacedBoatBlockId(@Nonnull String blockId) {
    return FishingBoatConstants.PLACED_BLOCK_ID.equals(blockId);
  }

  public static boolean placeParkedBoatBlock(
      @Nonnull World world,
      int x,
      int y,
      int z,
      float yawRadians
  ) {
    int topWaterY = BoatWaterHelper.topWaterBlockY(world, x, z);
    if (topWaterY == Integer.MIN_VALUE || y != topWaterY + 1) {
      return false;
    }

    WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
    if (chunk == null) {
      return false;
    }
    if (chunk.getBlock(x, y, z) != BlockType.EMPTY_ID) {
      return false;
    }

    int blockIndex = BlockType.getAssetMap().getIndex(FishingBoatConstants.PLACED_BLOCK_ID);
    BlockType blockType = BlockType.getAssetMap().getAsset(blockIndex);
    if (blockType == null || blockIndex == BlockType.UNKNOWN_ID) {
      return false;
    }

    int rotationIndex = rotationIndexFromYaw(yawRadians);
    chunk.setBlock(
        x,
        y,
        z,
        blockIndex,
        blockType,
        rotationIndex,
        FillerBlockUtil.NO_FILLER,
        SetBlockSettings.PERFORM_BLOCK_UPDATE
    );
    return true;
  }

  @Nonnull
  public static org.joml.Vector3d standPositionOnParkedBlock(int blockX, int parkedBlockY, int blockZ) {
    return new org.joml.Vector3d(blockX + 0.5, parkedBlockY + 1.0, blockZ + 0.5);
  }

  public static boolean removeBoatBlockKeepingWater(@Nonnull World world, int x, int y, int z) {
    WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
    if (chunk == null) {
      return false;
    }

    int blockIndex = BlockType.getAssetMap().getIndex(FishingBoatConstants.PLACED_BLOCK_ID);
    if (chunk.getBlock(x, y, z) != blockIndex) {
      return false;
    }

    chunk.setBlock(
        x,
        y,
        z,
        BlockType.EMPTY_ID,
        BlockType.EMPTY,
        RotationTuple.NONE_INDEX,
        FillerBlockUtil.NO_FILLER,
        SetBlockSettings.PERFORM_BLOCK_UPDATE
    );
    return true;
  }

  public static float yawFromBlockRotation(@Nonnull World world, @Nonnull Vector3i blockPos) {
    int x = blockPos.x;
    int y = blockPos.y;
    int z = blockPos.z;
    if (y < 0 || y >= ChunkUtil.HEIGHT) {
      return 0.0f;
    }
    var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
    if (sectionRef == null) {
      return 0.0f;
    }
    BlockSection blockSection =
        world.getChunkStore().getStore().getComponent(sectionRef, BlockSection.getComponentType());
    if (blockSection == null) {
      return 0.0f;
    }
    return (float) RotationTuple.get(blockSection.getRotationIndex(x, y, z)).yaw().getRadians();
  }

  public static int rotationIndexFromYaw(float yawRadians) {
    float degrees = (float) Math.toDegrees(yawRadians);
    degrees = (degrees % 360.0f + 360.0f) % 360.0f;
    Rotation rotation;
    if (degrees >= 45.0f && degrees < 135.0f) {
      rotation = Rotation.Ninety;
    } else if (degrees >= 135.0f && degrees < 225.0f) {
      rotation = Rotation.OneEighty;
    } else if (degrees >= 225.0f && degrees < 315.0f) {
      rotation = Rotation.TwoSeventy;
    } else {
      rotation = Rotation.None;
    }
    return RotationTuple.of(rotation, Rotation.None).index();
  }

  /** Block model forward is opposite the NPC entity model forward. */
  public static float entityYawToParkedBlockYaw(float entityYawRadians) {
    return entityYawRadians + (float) Math.PI;
  }
}
