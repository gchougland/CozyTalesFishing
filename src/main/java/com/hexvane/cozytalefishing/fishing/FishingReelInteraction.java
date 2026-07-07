package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
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
        FishingReelHold.tick(this, super::tick0, firstRun, time, type, context, cooldownHandler);
    }

    @Override
    protected void simulateTick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        FishingReelHold.simulateTick(super::simulateTick0, firstRun, time, type, context, cooldownHandler);
    }
}
