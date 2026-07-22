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

/** Distinguishes water, lava, and registered mod fluids for fishing spawn columns. */
public final class FishFluidHelper {
    public enum Kind {
        WATER,
        LAVA,
        REGISTERED
    }

    public record ColumnFluid(@Nonnull Kind kind, @Nullable String fluidAssetId) {
        public static final ColumnFluid WATER = new ColumnFluid(Kind.WATER, null);
        public static final ColumnFluid LAVA = new ColumnFluid(Kind.LAVA, null);

        @Nonnull
        public static ColumnFluid registered(@Nonnull String fluidAssetId) {
            return new ColumnFluid(Kind.REGISTERED, fluidAssetId);
        }

        public boolean isWater() {
            return kind == Kind.WATER;
        }

        public boolean isLava() {
            return kind == Kind.LAVA;
        }

        public boolean isRegistered() {
            return kind == Kind.REGISTERED;
        }

        /** When {@code filter} is null, any fishable fluid matches. */
        public boolean matchesFilter(@Nullable ColumnFluid filter) {
            if (filter == null) {
                return true;
            }
            if (kind != filter.kind) {
                return false;
            }
            if (kind == Kind.REGISTERED) {
                return fluidAssetId != null && fluidAssetId.equalsIgnoreCase(filter.fluidAssetId);
            }
            return true;
        }

        public boolean sameAs(@Nullable ColumnFluid other) {
            if (other == null) {
                return false;
            }
            return matchesFilter(other) && other.matchesFilter(this);
        }

        @Nonnull
        public String displayLabel() {
            return switch (kind) {
                case WATER -> "WATER";
                case LAVA -> "LAVA";
                case REGISTERED -> fluidAssetId != null ? fluidAssetId : "REGISTERED";
            };
        }
    }

    private FishFluidHelper() {}

    @Nullable
    public static ColumnFluid fluidTypeAt(@Nonnull World world, int x, int y, int z) {
        return fluidTypeFromIndex(fluidIdAt(world, x, y, z));
    }

    @Nullable
    public static ColumnFluid fluidTypeFromIndex(int fluidId) {
        if (fluidId == Fluid.EMPTY_ID) {
            return null;
        }
        String assetId = FishableFluidRegistry.fluidAssetIdFromIndex(fluidId);
        if (assetId == null) {
            return null;
        }
        String id = assetId.toLowerCase();
        if (id.contains("lava")) {
            return ColumnFluid.LAVA;
        }
        if (id.contains("water")) {
            return ColumnFluid.WATER;
        }
        if (FishableFluidRegistry.isRegisteredFluidId(assetId)) {
            return ColumnFluid.registered(assetId);
        }
        return null;
    }

    @Nullable
    public static String fluidAssetIdAt(@Nonnull World world, int x, int y, int z) {
        return FishableFluidRegistry.fluidAssetIdFromIndex(fluidIdAt(world, x, y, z));
    }

    public static boolean isWaterFluidAt(@Nonnull World world, int x, int y, int z) {
        ColumnFluid fluid = fluidTypeAt(world, x, y, z);
        return fluid != null && fluid.isWater();
    }

    public static boolean isLavaFluidAt(@Nonnull World world, int x, int y, int z) {
        ColumnFluid fluid = fluidTypeAt(world, x, y, z);
        return fluid != null && fluid.isLava();
    }

    public static boolean isFishableFluidAt(@Nonnull World world, int x, int y, int z) {
        return fluidTypeAt(world, x, y, z) != null;
    }

    public static boolean isFishableFluidId(int fluidId) {
        return FishableFluidRegistry.isFishableFluidId(fluidId);
    }

    /**
     * Raycasts from the player's look for a fishable water/lava/custom cell (including empty blocks that hold fluid).
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

    @Nonnull
    public static ColumnFluid columnFluidFor(@Nonnull WaterBodyType bodyType, @Nullable String registeredFluidAssetId) {
        if (registeredFluidAssetId != null && FishableFluidRegistry.isRegisteredFluidId(registeredFluidAssetId)) {
            return ColumnFluid.registered(registeredFluidAssetId);
        }
        return columnFluidFor(bodyType);
    }

    @Nonnull
    public static ColumnFluid columnFluidForShadow(@Nonnull FishShadowComponent shadow) {
        return columnFluidFor(shadow.getWaterBodyType(), shadow.getColumnFluidAssetId());
    }

    static int fluidIdAt(@Nonnull World world, int x, int y, int z) {
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
