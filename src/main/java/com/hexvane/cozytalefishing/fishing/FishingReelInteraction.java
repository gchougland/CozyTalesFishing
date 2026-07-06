package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Hold primary to reel the bobber toward the player or fight a hooked fish. */
public final class FishingReelInteraction extends ChargingInteraction {
    @Nonnull
    public static final BuilderCodec<FishingReelInteraction> CODEC =
        BuilderCodec.builder(FishingReelInteraction.class, FishingReelInteraction::new, ChargingInteraction.CODEC)
            .documentation("Hold to reel in the bobber or fight a hooked fish.")
            .build();

    @Override
    protected void tick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer != null && playerRef != null) {
            FishingLineComponent line = FishingLineService.getOrCreateLine(commandBuffer, playerRef);
            line.setReeling(true);
            commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }
        super.tick0(firstRun, time, type, context, cooldownHandler);
    }

    @Override
    protected void simulateTick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer != null && playerRef != null) {
            FishingLineComponent line = FishingLineService.getLine(commandBuffer, playerRef);
            if (line != null) {
                line.setReeling(false);
                commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
            }
        }
        if (context.getState().state == InteractionState.Finished) {
            stopReeling(context);
        }
        super.simulateTick0(firstRun, time, type, context, cooldownHandler);
    }

    private static void stopReeling(@Nonnull InteractionContext context) {
        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer == null || playerRef == null) {
            return;
        }
        FishingLineComponent line = FishingLineService.getLine(commandBuffer, playerRef);
        if (line != null) {
            line.setReeling(false);
            commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }
    }
}
