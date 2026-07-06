package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Plays cast animation; bobber spawn is handled by {@link FishingChargeInteraction}. */
public final class FishingCastInteraction extends SimpleInteraction {
    @Nonnull
    public static final BuilderCodec<FishingCastInteraction> CODEC =
        BuilderCodec.builder(FishingCastInteraction.class, FishingCastInteraction::new, SimpleInteraction.CODEC)
            .documentation("Cast animation after cozy fishing charge; spawn occurs in charge interaction.")
            .build();

    @Override
    protected void tick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        if (Boolean.TRUE.equals(context.getInstanceStore().getMetaObject(FishingChargeInteraction.BOBBER_SPAWNED))) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer != null && FishingLineService.hasCastOut(commandBuffer, playerRef)) {
            context.getInstanceStore().putMetaObject(FishingChargeInteraction.BOBBER_SPAWNED, Boolean.TRUE);
            context.getState().state = InteractionState.Finished;
            return;
        }

        float chargeValue = resolveChargeSeconds(context);
        if (chargeValue >= FishingConstants.MIN_CAST_CHARGE_SECONDS) {
            FishingDebugLog.info("Cast fallback spawn (charge=%.3fs time=%.3fs)", chargeValue, time);
            FishingChargeInteraction.trySpawnOnRelease(context, chargeValue);
            context.getState().state = InteractionState.Finished;
            return;
        }

        if (time > 2.0f) {
            FishingDebugLog.warn(
                "Cast timed out (time=%.3fs charge=%.3fs lastCharge=%.3fs)",
                time,
                chargeValue,
                context.getInstanceStore().getMetaObject(FishingChargeInteraction.LAST_CHARGE_SECONDS)
            );
            context.getState().state = InteractionState.Failed;
            return;
        }

        if (firstRun) {
            FishingDebugLog.info("Cast waiting (charge=%.3fs time=%.3fs)", chargeValue, time);
        }
        context.getState().state = InteractionState.NotFinished;
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

    private static float resolveChargeSeconds(@Nonnull InteractionContext context) {
        Float release = context.getInstanceStore().getMetaObject(FishingChargeInteraction.RELEASE_CHARGE_SECONDS);
        if (release != null && release >= FishingConstants.MIN_CAST_CHARGE_SECONDS) {
            return release;
        }

        Float elapsed = context.getInstanceStore().getMetaObject(FishingChargeInteraction.LAST_CHARGE_SECONDS);
        if (elapsed != null && elapsed >= FishingConstants.MIN_CAST_CHARGE_SECONDS) {
            return Math.min(elapsed, FishingConstants.MAX_CHARGE_SECONDS);
        }

        if (context.getState().chargeValue >= FishingConstants.MIN_CAST_CHARGE_SECONDS) {
            return Math.min(context.getState().chargeValue, FishingConstants.MAX_CHARGE_SECONDS);
        }

        InteractionSyncData clientState = context.getClientState();
        if (clientState != null && clientState.chargeValue >= FishingConstants.MIN_CAST_CHARGE_SECONDS) {
            return Math.min(clientState.chargeValue, FishingConstants.MAX_CHARGE_SECONDS);
        }

        return -1.0f;
    }
}
