package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Distinguishes water from lava (and other fluids) for fishing spawn columns. */
public final class FishFluidHelper {
    public enum ColumnFluid {
        WATER,
        LAVA
    }

    private FishFluidHelper() {}

    @Nullable
    public static ColumnFluid fluidTypeAt(@Nonnull World world, int x, int y, int z) {
        int fluidId = fluidIdAt(world, x, y, z);
        if (fluidId == Fluid.EMPTY_ID) {
            return null;
        }
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        if (fluid == null || fluid.getId() == null) {
            return null;
        }
        String id = fluid.getId().toLowerCase();
        if (id.contains("lava")) {
            return ColumnFluid.LAVA;
        }
        if (id.contains("water")) {
            return ColumnFluid.WATER;
        }
        return null;
    }

    public static boolean isWaterFluidAt(@Nonnull World world, int x, int y, int z) {
        return fluidTypeAt(world, x, y, z) == ColumnFluid.WATER;
    }

    public static boolean isLavaFluidAt(@Nonnull World world, int x, int y, int z) {
        return fluidTypeAt(world, x, y, z) == ColumnFluid.LAVA;
    }

    public static boolean isFishableFluidAt(@Nonnull World world, int x, int y, int z) {
        return fluidTypeAt(world, x, y, z) != null;
    }

    public static boolean isFishableFluidId(int fluidId) {
        if (fluidId == Fluid.EMPTY_ID) {
            return false;
        }
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        if (fluid == null || fluid.getId() == null) {
            return false;
        }
        String id = fluid.getId().toLowerCase();
        return id.contains("water") || id.contains("lava");
    }

    /**
     * Raycasts from the player's look for a fishable water/lava cell (including empty blocks that hold fluid).
     */
    @Nullable
    public static Vector3i raycastFishableFluidTarget(
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
                return isFishableFluidId(fluidId);
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

    @Nonnull
    public static ColumnFluid columnFluidFor(@Nonnull WaterBodyType bodyType) {
        return bodyType == WaterBodyType.Lava ? ColumnFluid.LAVA : ColumnFluid.WATER;
    }

    private static int fluidIdAt(@Nonnull World world, int x, int y, int z) {
        var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return Fluid.EMPTY_ID;
        }
        FluidSection fluidSection =
            world.getChunkStore().getStore().getComponentConcurrent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return Fluid.EMPTY_ID;
        }
        return fluidSection.getFluidId(x, y, z);
    }
}
