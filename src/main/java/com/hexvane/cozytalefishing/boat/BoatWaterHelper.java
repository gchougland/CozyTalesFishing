package com.hexvane.cozytalefishing.boat;



import com.hexvane.cozytalefishing.fish.FishFluidHelper;

import com.hexvane.cozytalefishing.fish.FishingModConfig;

import com.hypixel.hytale.component.ComponentAccessor;

import com.hypixel.hytale.component.Ref;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;

import com.hypixel.hytale.server.core.entity.InteractionManager;

import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import com.hypixel.hytale.server.core.universe.world.World;

import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import org.joml.Vector3d;

import org.joml.Vector3i;



/** Water placement and buoyancy helpers for fishing boats. */

public final class BoatWaterHelper {

  private static final int SCAN_MIN_Y = 0;

  private static final int SCAN_MAX_Y = 319;



  private BoatWaterHelper() {}



  public record WaterPlacement(int blockX, int blockY, int blockZ, double surfaceY) {}



  @Nonnull

  public static WaterPlacement resolveWaterPlacement(@Nonnull World world, int x, int y, int z) {

    if (FishFluidHelper.isWaterFluidAt(world, x, y, z)) {

      return new WaterPlacement(x, y, z, entityDeckYAt(world, x, z));

    }

    if (FishFluidHelper.isWaterFluidAt(world, x, y - 1, z)) {

      return new WaterPlacement(x, y - 1, z, entityDeckYAt(world, x, z));

    }

    if (FishFluidHelper.isWaterFluidAt(world, x, y + 1, z)) {

      return new WaterPlacement(x, y + 1, z, entityDeckYAt(world, x, z));

    }

    return new WaterPlacement(x, y, z, Double.NaN);

  }



  public static boolean isValidPlacement(@Nonnull World world, @Nonnull WaterPlacement placement) {

    if (Double.isNaN(placement.surfaceY())) {

      return false;

    }

    if (FishFluidHelper.isLavaFluidAt(world, placement.blockX(), placement.blockY(), placement.blockZ())) {

      return false;

    }

    return hasMinWaterDepth(world, placement.blockX(), placement.blockZ());

  }



  public static boolean isOverWater(@Nonnull World world, double x, double y, double z) {

    int blockX = (int) Math.floor(x);

    int blockZ = (int) Math.floor(z);

    return topWaterBlockY(world, blockX, blockZ) != Integer.MIN_VALUE;

  }



  public static int topWaterBlockY(@Nonnull World world, int blockX, int blockZ) {

    int topWaterY = Integer.MIN_VALUE;

    for (int y = SCAN_MIN_Y; y <= SCAN_MAX_Y; y++) {

      if (FishFluidHelper.isWaterFluidAt(world, blockX, y, blockZ)) {

        topWaterY = y;

      }

    }

    return topWaterY;

  }



  public static int parkedBlockY(@Nonnull World world, int blockX, int blockZ) {

    int topWaterY = topWaterBlockY(world, blockX, blockZ);

    return topWaterY == Integer.MIN_VALUE ? Integer.MIN_VALUE : topWaterY + 1;

  }



  public static double entityDeckYAt(@Nonnull World world, int blockX, int blockZ) {
    int topWaterY = topWaterBlockY(world, blockX, blockZ);
    if (topWaterY == Integer.MIN_VALUE) {
      return Double.NaN;
    }
    return topWaterY + 1.0;
  }

  public static double deckYForPosition(@Nonnull World world, double x, double z) {
    return entityDeckYAt(world, (int) Math.floor(x), (int) Math.floor(z));
  }

  /** Returns true when the boat entity is supported by water at its current position. */
  public static boolean isFloatingOnWater(@Nonnull World world, double x, double y, double z) {
    if (Double.isNaN(deckYForPosition(world, x, z))) {
      return false;
    }

    int blockX = (int) Math.floor(x);
    int blockY = (int) Math.floor(y);
    int blockZ = (int) Math.floor(z);

    return FishFluidHelper.isWaterFluidAt(world, blockX, blockY, blockZ)
        || FishFluidHelper.isWaterFluidAt(world, blockX, blockY - 1, blockZ);
  }



  public static boolean isWaterTargetBlock(@Nonnull World world, @Nonnull Vector3i targetBlock) {

    int blockId = world.getBlock(targetBlock.x, targetBlock.y, targetBlock.z);

    if (blockId != BlockType.EMPTY_ID) {

      return false;

    }

    return FishFluidHelper.isWaterFluidAt(world, targetBlock.x, targetBlock.y, targetBlock.z);

  }



  @Nullable

  public static Vector3i raycastWaterTarget(

      @Nonnull World world,

      @Nonnull Ref<EntityStore> ref,

      @Nonnull ComponentAccessor<EntityStore> componentAccessor

  ) {

    var look = TargetUtil.getLook(ref, componentAccessor);

    var pos = look.getPosition();

    var dir = look.getDirection();

    return TargetUtil.getTargetBlock(

        world,

        (blockId, fluidId) -> {

          if (blockId != BlockType.EMPTY_ID) {

            return true;

          }

          return isWaterFluidId(fluidId);

        },

        pos.x,

        pos.y,

        pos.z,

        dir.x,

        dir.y,

        dir.z,

        InteractionManager.MAX_REACH_DISTANCE

    );

  }



  private static boolean isWaterFluidId(int fluidId) {

    if (fluidId == Fluid.EMPTY_ID) {

      return false;

    }

    Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);

    if (fluid == null || fluid.getId() == null) {

      return false;

    }

    return fluid.getId().toLowerCase().contains("water");

  }



  private static boolean hasMinWaterDepth(@Nonnull World world, int x, int z) {

    int required = Math.max(1, FishingModConfig.get().getBoatPlacementMinWaterDepth());

    int topWaterY = topWaterBlockY(world, x, z);

    if (topWaterY == Integer.MIN_VALUE) {

      return false;

    }



    int depth = 0;

    for (int y = topWaterY; y >= SCAN_MIN_Y; y--) {

      if (!FishFluidHelper.isWaterFluidAt(world, x, y, z)) {

        break;

      }

      depth++;

      if (depth >= required) {

        return true;

      }

    }

    return false;

  }

}


