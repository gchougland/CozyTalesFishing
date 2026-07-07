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
 * Primary fishing rod click: delegates to the hold-to-reel root when a line is active,
 * otherwise starts the charge/cast chain.
 */
public final class FishingPrimaryInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<FishingPrimaryInteraction> CODEC =
        BuilderCodec.builder(FishingPrimaryInteraction.class, FishingPrimaryInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Primary cozy fishing rod interaction — reel root if cast out, else charge to cast.")
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
                new KeyedCodec<>("ReelRoot", Codec.STRING),
                (interaction, root) -> interaction.reelRoot = root,
                interaction -> interaction.reelRoot,
                (interaction, parent) -> interaction.reelRoot = parent.reelRoot
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
    protected String reelRoot;
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

        if (commandBuffer == null || playerRef == null) {
            return;
        }

        FishingLineService.sanitizeStaleLineState(commandBuffer, playerRef);
        FishingReelHold.clearReeling(context);

        if (FishingLineService.hasCastOut(commandBuffer, playerRef)) {
            if (reelRoot != null) {
                FishingDebugLog.info("Primary click: starting reel root %s", reelRoot);
                var reelRootInteraction = RootInteraction.getAssetMap().getAsset(reelRoot);
                if (reelRootInteraction != null) {
                    context.execute(reelRootInteraction);
                }
            }
            return;
        }

        if (chargeRoot != null) {
            FishingDebugLog.info("Primary click: starting charge root %s", chargeRoot);
            var chargeRootInteraction = RootInteraction.getAssetMap().getAsset(chargeRoot);
            if (chargeRootInteraction != null) {
                context.execute(chargeRootInteraction);
            }
        }
    }

    @Override
    protected void simulateFirstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        context.getState().state = InteractionState.Finished;
        FishingReelHold.clearReeling(context);
    }
}
