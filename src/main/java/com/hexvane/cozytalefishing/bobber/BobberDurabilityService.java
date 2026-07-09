package com.hexvane.cozytalefishing.bobber;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class BobberDurabilityService {
    private BobberDurabilityService() {}

    public static void applyCatchWear(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        ItemStack rodStack = InventoryComponent.getItemInHand(store, playerRef);
        if (rodStack == null || ItemStack.isEmpty(rodStack)) {
            return;
        }

        ItemStack[] slots = BobberLoadoutService.getEquippedSlots(rodStack);
        boolean changed = false;
        for (int i = 0; i < slots.length; i++) {
            ItemStack bobber = slots[i];
            if (ItemStack.isEmpty(bobber) || bobber.isBroken() || bobber.isUnbreakable()) {
                continue;
            }
            slots[i] = bobber.withIncreasedDurability(-1.0);
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
}
