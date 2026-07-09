package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Shared hold-to-reel logic for charging-style fishing rod interactions. */
final class FishingReelHold {
    private static final float CHARGING_CANCELED = -2.0f;

    @FunctionalInterface
    interface SuperTick {
        void tick(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler
        );
    }

    private FishingReelHold() {}

    static void tick(
        @Nonnull ChargingInteraction interaction,
        @Nonnull SuperTick superTick,
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        InteractionSyncData clientData = context.getClientState();
        if (firstRun && clientData != null) {
            float chargeValue = clientData.chargeValue;
            if (chargeValue >= 0.0f && chargeValue != CHARGING_CANCELED) {
                var commandBuffer = context.getCommandBuffer();
                Ref<EntityStore> playerRef = context.getEntity();
                if (commandBuffer != null && playerRef != null && !FishingLineService.hasCastOut(commandBuffer, playerRef)) {
                    context.getState().state = InteractionState.Finished;
                    setReeling(context, false);
                    return;
                }
                context.getState().state = InteractionState.NotFinished;
                setReeling(context, false);
                return;
            }
        }

        superTick.tick(firstRun, time, type, context, cooldownHandler);
        finishReelIfCastEnded(context);
        setReeling(context, context.getState().state != InteractionState.Finished);
    }

    static void simulateTick(
        @Nonnull SuperTick superTick,
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        superTick.tick(firstRun, time, type, context, cooldownHandler);
        finishReelIfCastEnded(context);
        if (context.getState().state == InteractionState.Finished) {
            clearReeling(context);
        }
    }

    static void clearReeling(@Nonnull InteractionContext context) {
        setReeling(context, false);
    }

    /** Stops hold-to-reel interactions so looping local reel audio is cleared on the client. */
    static void cancelActiveReelInteraction(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        InteractionManager manager = accessor.getComponent(playerRef, InteractionModule.get().getInteractionManagerComponent());
        if (manager == null) {
            return;
        }

        List<InteractionChain> toCancel = new ArrayList<>();
        manager.forEachInteraction(
            (chain, interaction, chains) -> {
                if (interaction instanceof FishingReelInteraction
                    && chain.getServerState() == InteractionState.NotFinished) {
                    chains.add(chain);
                }
                return chains;
            },
            toCancel
        );

        for (InteractionChain chain : toCancel) {
            manager.cancelChains(chain);
        }
    }

    private static void finishReelIfCastEnded(@Nonnull InteractionContext context) {
        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer == null || playerRef == null) {
            return;
        }
        if (!FishingLineService.hasCastOut(commandBuffer, playerRef)) {
            context.getState().state = InteractionState.Finished;
        }
    }

    private static void setReeling(@Nonnull InteractionContext context, boolean reeling) {
        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer == null || playerRef == null) {
            return;
        }
        FishingLineComponent line = FishingLineService.getLine(commandBuffer, playerRef);
        if (line == null) {
            return;
        }
        if (reeling && !FishingLineService.hasCastOut(commandBuffer, playerRef)) {
            reeling = false;
        }
        if (line.isReeling() != reeling) {
            line.setReeling(reeling);
            commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }
    }
}
