package com.hexvane.cozytalefishing.treasure;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import java.util.List;
import javax.annotation.Nonnull;

/** Consumes a sunken treasure item and grants rolled loot from an ItemDropList. */
public final class OpenSunkenTreasureInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<OpenSunkenTreasureInteraction> CODEC =
        BuilderCodec.builder(OpenSunkenTreasureInteraction.class, OpenSunkenTreasureInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Opens sunken treasure, rolling loot and granting items to the player.")
            .appendInherited(
                new KeyedCodec<>("DropListId", Codec.STRING),
                (interaction, id) -> interaction.dropListId = id,
                interaction -> interaction.dropListId,
                (interaction, parent) -> interaction.dropListId = parent.dropListId
            )
            .add()
            .build();

    private String dropListId = SunkenTreasureConstants.DROP_LIST_ID;

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
        if (heldItem == null || heldItem.isEmpty() || !SunkenTreasureConstants.ITEM_ID.equals(heldItem.getItemId())) {
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

        final var store = commandBuffer.getStore();
        final var inventory = InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.HOTBAR_FIRST);
        final String listId =
            dropListId != null && !dropListId.isBlank() ? dropListId : SunkenTreasureConstants.DROP_LIST_ID;
        final List<ItemStack> loot = ItemModule.get().getRandomItemDrops(listId);
        SimpleItemContainer.addOrDropItemStacks(store, ref, inventory, loot);

        final var world = commandBuffer.getExternalData().getWorld();
        final boolean showNotifications = world.getGameplayConfig().getShowItemPickupNotifications();
        final PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(Message.translation("server.cozytalefishing.treasure.opened"));
            for (final ItemStack stack : loot) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                if (showNotifications) {
                    NotificationUtil.sendNotification(
                        playerRef.getPacketHandler(),
                        Message.translation("server.cozytalefishing.treasure.received").param("item", stack.getDisplayName()),
                        null,
                        stack.toPacket()
                    );
                }
            }
        }
    }
}
