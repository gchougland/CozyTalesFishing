package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Recalls an active fishing line and despawns bobber + string segments. */
public final class FishingRecallInteraction extends SimpleInteraction {
    @Nonnull
    public static final BuilderCodec<FishingRecallInteraction> CODEC =
        BuilderCodec.builder(FishingRecallInteraction.class, FishingRecallInteraction::new, SimpleInteraction.CODEC)
            .documentation("Recalls the active cozy fishing line.")
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
        if (firstRun && commandBuffer != null) {
            Ref<EntityStore> playerRef = context.getEntity();
            FishingLineService.recallCastOut(commandBuffer, playerRef);
        }

        if (time >= 0.25f) {
            context.getState().state = InteractionState.Finished;
        } else {
            context.getState().state = InteractionState.NotFinished;
        }
    }

    @Override
    protected void simulateTick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        context.getState().state = InteractionState.Finished;
    }
}
