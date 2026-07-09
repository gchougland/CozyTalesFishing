package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    @Nonnull
    public static ColumnFluid columnFluidFor(@Nonnull WaterBodyType bodyType) {
        return bodyType == WaterBodyType.Lava ? ColumnFluid.LAVA : ColumnFluid.WATER;
    }

    private static int fluidIdAt(@Nonnull World world, int x, int y, int z) {
        var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return Fluid.EMPTY_ID;
        }
        FluidSection fluidSection = world.getChunkStore().getStore().getComponentConcurrent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return Fluid.EMPTY_ID;
        }
        return fluidSection.getFluidId(x, y, z);
    }
}
