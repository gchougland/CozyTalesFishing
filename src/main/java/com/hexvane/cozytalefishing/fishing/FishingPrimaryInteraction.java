package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Primary fishing rod click: reel in when a line is active, otherwise start the charge/cast chain.
 */
public final class FishingPrimaryInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<FishingPrimaryInteraction> CODEC =
        BuilderCodec.builder(FishingPrimaryInteraction.class, FishingPrimaryInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Primary cozy fishing rod interaction — recall if cast out, else charge to cast.")
            .appendInherited(
                new KeyedCodec<>("ChargeRoot", Codec.STRING),
                (interaction, root) -> interaction.chargeRoot = root,
                interaction -> interaction.chargeRoot,
                (interaction, parent) -> interaction.chargeRoot = parent.chargeRoot
            )
            .addValidator(Validators.nonNull())
            .addValidatorLate(() -> RootInteraction.VALIDATOR_CACHE.getValidator().late())
            .add()
            .appendInherited(
                new KeyedCodec<>("RecallRoot", Codec.STRING),
                (interaction, root) -> interaction.recallRoot = root,
                interaction -> interaction.recallRoot,
                (interaction, parent) -> interaction.recallRoot = parent.recallRoot
            )
            .addValidator(Validators.nonNull())
            .addValidatorLate(() -> RootInteraction.VALIDATOR_CACHE.getValidator().late())
            .add()
            .build();

    protected String chargeRoot;
    protected String recallRoot;

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        var commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> playerRef = context.getEntity();
        context.getState().state = InteractionState.Finished;

        if (commandBuffer != null && FishingLineService.hasCastOut(commandBuffer, playerRef)) {
            if (recallRoot == null) {
                FishingDebugLog.warn("Primary click: no RecallRoot configured, recalling without animation");
                FishingLineService.recallCastOut(commandBuffer, playerRef);
                return;
            }
            FishingDebugLog.info("Primary click: starting recall root %s", recallRoot);
            context.execute(RootInteraction.getRootInteractionOrUnknown(recallRoot));
            return;
        }

        if (chargeRoot == null) {
            FishingDebugLog.warn("Primary click: no ChargeRoot configured, cannot start charge");
            return;
        }

        FishingDebugLog.info("Primary click: starting charge root %s", chargeRoot);
        context.execute(RootInteraction.getRootInteractionOrUnknown(chargeRoot));
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
