package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import javax.annotation.Nonnull;

/** Fluid ticker that keeps source levels without spreading into adjacent blocks. */
public final class StaticFluidTicker extends FluidTicker {
    public static final BuilderCodec<StaticFluidTicker> CODEC =
        BuilderCodec.builder(StaticFluidTicker.class, StaticFluidTicker::new, BASE_CODEC).build();

    public static final StaticFluidTicker INSTANCE = new StaticFluidTicker();

    private StaticFluidTicker() {}

    @Override
    public boolean canOccupySolidBlocks() {
        return true;
    }

    @Nonnull
    @Override
    protected BlockTickStrategy spread(
        World world,
        long tick,
        @Nonnull Accessor accessor,
        @Nonnull FluidSection fluidSection,
        BlockSection blockSection,
        @Nonnull Fluid fluid,
        int fluidId,
        byte fluidLevel,
        int worldX,
        int worldY,
        int worldZ
    ) {
        return BlockTickStrategy.SLEEP;
    }
}
