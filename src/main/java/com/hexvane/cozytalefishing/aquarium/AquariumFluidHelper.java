package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Places and removes aquarium-only water inside tank footprints. */
public final class AquariumFluidHelper {
    public static final String AQUARIUM_WATER_FLUID_ID = "CozyFishing_Aquarium_Water";

    private static Integer cachedWaterFluidId;

    private AquariumFluidHelper() {}

    public static void setWaterInFootprint(@Nonnull World world, @Nonnull Vector3i originBlock) {
        AquariumBlockHelper.forEachFootprintBlock(
            world,
            originBlock,
            (x, y, z) -> setWaterAt(world, x, y, z)
        );
    }

    public static void setWaterInFootprint(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull com.hexvane.cozytalefishing.fish.AquariumSize size,
        int rotationIndex
    ) {
        AquariumBlockHelper.forEachFootprintBySize(
            world,
            originBlock,
            size,
            rotationIndex,
            (x, y, z) -> setWaterAt(world, x, y, z)
        );
    }

    public static void reconcileWaterInFootprint(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull com.hexvane.cozytalefishing.fish.AquariumSize size,
        int rotationIndex
    ) {
        setWaterInFootprint(world, originBlock, size, rotationIndex);
    }

    public static void clearWaterInFootprint(@Nonnull World world, @Nonnull Vector3i originBlock) {
        clearWaterInFootprint(
            world,
            originBlock,
            com.hexvane.cozytalefishing.fish.AquariumSize.Small,
            AquariumBlockHelper.rotationIndexAt(world, originBlock)
        );
    }

    public static void clearWaterInFootprint(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nullable com.hexvane.cozytalefishing.fish.AquariumSize size,
        int rotationIndex
    ) {
        com.hexvane.cozytalefishing.fish.AquariumSize resolvedSize =
            size != null ? size : com.hexvane.cozytalefishing.fish.AquariumSize.Small;
        AquariumBlockHelper.forEachFootprintBySize(
            world,
            originBlock,
            resolvedSize,
            rotationIndex,
            (x, y, z) -> clearWaterAt(world, x, y, z)
        );
    }

    public static boolean hasWaterInFootprint(
        @Nonnull World world,
        @Nonnull Vector3i originBlock,
        @Nonnull com.hexvane.cozytalefishing.fish.AquariumSize size,
        int rotationIndex
    ) {
        int expectedFluidId = waterFluidId();
        int[] checked = {0};
        int[] filled = {0};
        AquariumBlockHelper.forEachFootprintBySize(
            world,
            originBlock,
            size,
            rotationIndex,
            (x, y, z) -> {
                checked[0]++;
                if (fluidIdAt(world, x, y, z) == expectedFluidId) {
                    filled[0]++;
                }
            }
        );
        return checked[0] > 0 && filled[0] == checked[0];
    }

    private static int waterFluidId() {
        Integer cached = cachedWaterFluidId;
        if (cached != null) {
            return cached;
        }

        int resolved =
            Fluid.getFluidIdOrUnknown(
                AQUARIUM_WATER_FLUID_ID,
                "Aquarium failed to resolve fluid '%s'",
                AQUARIUM_WATER_FLUID_ID
            );
        cachedWaterFluidId = resolved;
        return resolved;
    }

    private static int fluidIdAt(@Nonnull World world, int blockX, int blockY, int blockZ) {
        var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(blockX, blockY, blockZ);
        if (sectionRef == null) {
            return Fluid.EMPTY_ID;
        }
        FluidSection fluidSection =
            world.getChunkStore().getStore().getComponentConcurrent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return Fluid.EMPTY_ID;
        }
        return fluidSection.getFluidId(blockX, blockY, blockZ);
    }

    private static void setWaterAt(@Nonnull World world, int blockX, int blockY, int blockZ) {
        int fluidId = waterFluidId();
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        if (fluid == null) {
            return;
        }

        if (fluidIdAt(world, blockX, blockY, blockZ) == fluidId) {
            return;
        }

        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(blockX, blockY, blockZ);
        if (sectionRef == null) {
            return;
        }
        var fluidSection = chunkStore.getStore().ensureAndGetComponent(sectionRef, FluidSection.getComponentType());
        fluidSection.setFluid(blockX, blockY, blockZ, fluidId, (byte) fluid.getMaxFluidLevel());
    }

    private static void clearWaterAt(@Nonnull World world, int blockX, int blockY, int blockZ) {
        if (fluidIdAt(world, blockX, blockY, blockZ) != waterFluidId()) {
            return;
        }

        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(blockX, blockY, blockZ);
        if (sectionRef == null) {
            return;
        }
        var fluidSection = chunkStore.getStore().ensureAndGetComponent(sectionRef, FluidSection.getComponentType());
        fluidSection.setFluid(blockX, blockY, blockZ, Fluid.EMPTY_ID, (byte) 0);
    }
}
