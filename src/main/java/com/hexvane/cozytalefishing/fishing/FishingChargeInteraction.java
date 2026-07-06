package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Charging interaction that ignores the first sync packet when it arrives as {@code chargeValue = 0}
 * before the client reports {@code CHARGING_HELD (-1)}, which otherwise finishes charging instantly.
 */
public final class FishingChargeInteraction extends ChargingInteraction {
    private static final float CHARGING_HELD = -1.0f;
    private static final float CHARGING_CANCELED = -2.0f;

    /** Charge seconds captured when the player releases. */
    public static final MetaKey<Float> RELEASE_CHARGE_SECONDS =
        Interaction.META_REGISTRY.registerMetaObject(ignored -> -1.0f);

    /** Latest server-side charge duration while holding (interaction time). */
    public static final MetaKey<Float> LAST_CHARGE_SECONDS =
        Interaction.META_REGISTRY.registerMetaObject(ignored -> 0.0f);

    public static final MetaKey<Boolean> BOBBER_SPAWNED =
        Interaction.META_REGISTRY.registerMetaObject(ignored -> Boolean.FALSE);

    @Nonnull
    public static final BuilderCodec<FishingChargeInteraction> CODEC =
        BuilderCodec.builder(FishingChargeInteraction.class, FishingChargeInteraction::new, ChargingInteraction.CODEC)
            .documentation("Cozy fishing rod charge; ignores the initial zero charge sync packet.")
            .build();

    @Override
    protected void tick0(
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
                FishingDebugLog.info(
                    "Charge ignoring initial release sync (chargeValue=%.3f, time=%.3f)",
                    chargeValue,
                    time
                );
                context.getState().state = InteractionState.NotFinished;
                return;
            }
            if (chargeValue == CHARGING_HELD) {
                context.getInstanceStore().putMetaObject(BOBBER_SPAWNED, Boolean.FALSE);
                context.getInstanceStore().putMetaObject(RELEASE_CHARGE_SECONDS, -1.0f);
                FishingDebugLog.info("Charge started (held sync, time=%.3f)", time);
            }
        }

        context.getInstanceStore().putMetaObject(LAST_CHARGE_SECONDS, time);

        float clientReleaseCharge = -1.0f;
        if (clientData != null && clientData.chargeValue >= 0.0f && clientData.chargeValue != CHARGING_CANCELED) {
            clientReleaseCharge = clientData.chargeValue;
        }

        InteractionState stateBefore = context.getState().state;
        super.tick0(firstRun, time, type, context, cooldownHandler);

        if (stateBefore != context.getState().state) {
            FishingDebugLog.info(
                "Charge state %s -> %s (time=%.3f clientCharge=%.3f)",
                stateBefore,
                context.getState().state,
                time,
                clientReleaseCharge
            );
        }

        if (context.getState().state != InteractionState.Finished) {
            return;
        }
        if (clientData != null && clientData.chargeValue == CHARGING_CANCELED) {
            return;
        }

        // Server elapsed hold time drives cast distance (client chargeValue can be normalized when DisplayProgress is on).
        float chargeSeconds = Math.min(time, FishingConstants.MAX_CHARGE_SECONDS);
        context.getState().chargeValue = chargeSeconds;
        context.getInstanceStore().putMetaObject(RELEASE_CHARGE_SECONDS, chargeSeconds);
        trySpawnOnRelease(context, chargeSeconds);
    }

    static void trySpawnOnRelease(@Nonnull InteractionContext context, float chargeSeconds) {
        if (Boolean.TRUE.equals(context.getInstanceStore().getMetaObject(BOBBER_SPAWNED))) {
            return;
        }
        if (chargeSeconds < FishingConstants.MIN_CAST_CHARGE_SECONDS) {
            FishingDebugLog.info("Charge release ignored: %.3fs below minimum cast charge", chargeSeconds);
            return;
        }

        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        if (commandBuffer == null) {
            FishingDebugLog.warn("Charge release spawn aborted: null command buffer");
            return;
        }
        if (FishingLineService.hasCastOut(commandBuffer, playerRef)) {
            FishingDebugLog.info("Charge release spawn skipped: cast already out");
            context.getInstanceStore().putMetaObject(BOBBER_SPAWNED, Boolean.TRUE);
            return;
        }

        float clampedCharge = Math.min(chargeSeconds, FishingConstants.MAX_CHARGE_SECONDS);
        FishingDebugLog.info("Charge release spawning bobber (charge=%.3fs clamped=%.3fs)", chargeSeconds, clampedCharge);
        Ref<EntityStore> bobberRef = FishingLineService.castLine(playerRef, commandBuffer, clampedCharge);
        if (bobberRef != null) {
            context.getInstanceStore().putMetaObject(BOBBER_SPAWNED, Boolean.TRUE);
        }
    }
}
