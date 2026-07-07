package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.FishShadowProximity;
import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Advances rod-tip bob phase while the fishing rod is held so casts stay aligned with the client animation. */
public final class FishingRodHoldTickSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Query<EntityStore> query = Query.and(Player.getComponentType());

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        if (!FishShadowProximity.isHoldingFishingRod(commandBuffer, playerRef)) {
            if (commandBuffer.getComponent(playerRef, FishingRodHoldComponent.getComponentType()) != null) {
                commandBuffer.removeComponent(playerRef, FishingRodHoldComponent.getComponentType());
            }
            if (FishingLineService.hasCastOut(commandBuffer, playerRef)) {
                FishingLineService.recallCastOut(commandBuffer, playerRef);
            }
            return;
        }

        FishingModConfig config = FishingModConfig.get();
        if (config.getRodTipBobAmplitude() <= 0.0f || config.getRodTipBobFrequency() <= 0.0f) {
            return;
        }

        FishingRodHoldComponent hold = commandBuffer.getComponent(playerRef, FishingRodHoldComponent.getComponentType());
        if (hold == null) {
            hold = new FishingRodHoldComponent();
            if (config.isRodTipBobSeedFromWorldTime()) {
                World world = store.getExternalData().getWorld();
                if (world != null) {
                    hold.setRodTipBobPhaseRadians(RodTipUtil.computeWorldTimeBobPhaseRadians(world, config, 1.0f));
                }
            }
            commandBuffer.putComponent(playerRef, FishingRodHoldComponent.getComponentType(), hold);
        }

        float animSpeed = reelAnimSpeedMultiplier(commandBuffer, playerRef, config);
        hold.advanceRodTipBobPhase((float) (Math.PI * 2.0 * config.getRodTipBobFrequency() * animSpeed * dt));
    }

    private static float reelAnimSpeedMultiplier(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull FishingModConfig config
    ) {
        FishingLineComponent line = commandBuffer.getComponent(playerRef, FishingLineComponent.getComponentType());
        if (line == null || !line.isReeling()) {
            return 1.0f;
        }
        return config.getRodTipBobReelAnimSpeed();
    }
}
