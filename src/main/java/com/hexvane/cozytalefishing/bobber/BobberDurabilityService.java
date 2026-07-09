package com.hexvane.cozytalefishing.bobber;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BobberDurabilityService {
    private BobberDurabilityService() {}

    public static void applyCatchWear(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        ItemStack rodStack = InventoryComponent.getItemInHand(store, playerRef);
        if (rodStack == null || ItemStack.isEmpty(rodStack)) {
            return;
        }

        ItemStack[] slots = BobberLoadoutService.getEquippedSlots(rodStack);
        boolean changed = false;
        PlayerRef player = store.getComponent(playerRef, PlayerRef.getComponentType());

        for (int i = 0; i < slots.length; i++) {
            ItemStack bobber = slots[i];
            if (ItemStack.isEmpty(bobber) || bobber.isBroken() || bobber.isUnbreakable()) {
                continue;
            }
            ItemStack worn = bobber.withIncreasedDurability(-1.0);
            if (!bobber.isBroken() && worn.isBroken()) {
                playItemBreakSound(player);
            }
            slots[i] = worn;
            changed = true;
        }

        if (!changed) {
            return;
        }

        ItemStack updatedRod = BobberLoadoutService.writeSlots(rodStack, slots);
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            return;
        }

        byte activeSlot = hotbar.getActiveSlot();
        ItemStack current = hotbar.getActiveItem();
        if (ItemStack.isEmpty(current)) {
            return;
        }

        hotbar.getInventory().replaceItemStackInSlot(activeSlot, current, updatedRod);
    }

    private static void playItemBreakSound(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        int soundEventIndex = SoundEvent.getAssetMap().getIndex("SFX_Item_Break");
        if (soundEventIndex == 0) {
            return;
        }
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.UI);
    }
}
