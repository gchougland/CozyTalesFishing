package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Instantly recalls an active fishing line and despawns bobber + string segments. */
public final class FishingRecallInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<FishingRecallInteraction> CODEC =
        BuilderCodec.builder(FishingRecallInteraction.class, FishingRecallInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Instantly recalls the active cozy fishing line.")
            .build();

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        context.getState().state = InteractionState.Finished;
        var commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }
        Ref<EntityStore> playerRef = context.getEntity();
        if (FishingLineService.recallCastOut(commandBuffer, playerRef)) {
            FishingDebugLog.info("Secondary click: instant recall");
        }
    }

    @Override
    protected void simulateFirstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        context.getState().state = InteractionState.Finished;
    }
}
