package com.hexvane.cozytalefishing.treasure;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Consumes a message in a bottle and hints a random uncaught fish or shows a flavor note. */
public final class OpenMessageInBottleInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<OpenMessageInBottleInteraction> CODEC =
        BuilderCodec.builder(OpenMessageInBottleInteraction.class, OpenMessageInBottleInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Opens a message in a bottle, hinting an undiscovered fish or revealing a flavor note.")
            .build();

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        final var ref = context.getEntity();
        final var commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }

        final ItemStack heldItem = context.getHeldItem();
        if (heldItem == null || heldItem.isEmpty() || !MessageInBottleConstants.ITEM_ID.equals(heldItem.getItemId())) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        final var removeTransaction =
            context.getHeldItemContainer().removeItemStackFromSlot(context.getHeldItemSlot(), heldItem, 1);
        if (!removeTransaction.succeeded()) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        context.setHeldItem(removeTransaction.getSlotAfter());

        FishCatchRecordComponent records = commandBuffer.getComponent(ref, FishCatchRecordComponent.getComponentType());
        if (records == null) {
            records = new FishCatchRecordComponent();
        }

        Message resultMessage = MessageInBottleHintService.useBottle(records);
        commandBuffer.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);

        final PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(resultMessage);
        }
    }
}
