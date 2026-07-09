package com.hexvane.cozytalefishing.bobber;

import com.hexvane.cozytalefishing.fishing.FishingRodRegistry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BobberLoadoutService {
    private BobberLoadoutService() {}

    public static int getSlotCount(@Nullable String rodItemId) {
        return FishingRodRegistry.getBobberSlotCount(rodItemId);
    }

    @Nonnull
    public static ItemStack[] getEquippedSlots(@Nonnull ItemStack rodStack) {
        int slotCount = getSlotCount(rodStack.getItemId());
        ItemStack[] stored = readStoredSlots(rodStack);
        ItemStack[] normalized = new ItemStack[slotCount];
        for (int i = 0; i < slotCount; i++) {
            if (i < stored.length && !ItemStack.isEmpty(stored[i])) {
                normalized[i] = stored[i];
            }
        }
        return normalized;
    }

    @Nonnull
    public static List<ItemStack> getActiveBobbers(@Nonnull ItemStack rodStack) {
        List<ItemStack> active = new ArrayList<>();
        for (ItemStack slot : getEquippedSlots(rodStack)) {
            if (!ItemStack.isEmpty(slot) && !slot.isBroken()) {
                active.add(slot);
            }
        }
        return active;
    }

    @Nullable
    public static ItemStack getPrimaryVisualBobber(@Nonnull ItemStack rodStack) {
        for (ItemStack slot : getEquippedSlots(rodStack)) {
            if (!ItemStack.isEmpty(slot) && !slot.isBroken()) {
                return slot;
            }
        }
        return null;
    }

    @Nonnull
    public static ItemStack writeSlots(@Nonnull ItemStack rodStack, @Nonnull ItemStack[] slots) {
        return rodStack.withMetadata(EquippedBobbersMetadata.KEYED_CODEC, new EquippedBobbersMetadata(slots));
    }

    public static boolean equipBobber(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ItemContext rodContext,
        int slotIndex,
        @Nonnull ItemContext bobberContext
    ) {
        ItemStack rodStack = rodContext.getItemStack();
        int slotCount = getSlotCount(rodStack.getItemId());
        if (slotIndex < 0 || slotIndex >= slotCount) {
            return false;
        }

        ItemStack bobberStack = bobberContext.getItemStack();
        if (ItemStack.isEmpty(bobberStack) || !BobberRegistry.isBobber(bobberStack.getItemId())) {
            return false;
        }

        ItemStack[] slots = getEquippedSlots(rodStack);
        ItemStack previous = slots[slotIndex];
        ItemContainer bobberContainer = bobberContext.getContainer();
        short bobberSlot = bobberContext.getSlot();

        ItemStackSlotTransaction remove =
            bobberContainer.removeItemStackFromSlot(bobberSlot, bobberStack, 1);
        if (!remove.succeeded()) {
            return false;
        }

        slots[slotIndex] = remove.getOutput();
        ItemStack updatedRod = writeSlots(rodStack, slots);
        ItemStackSlotTransaction rodReplace =
            rodContext.getContainer().replaceItemStackInSlot(rodContext.getSlot(), rodStack, updatedRod);
        if (!rodReplace.succeeded()) {
            SimpleItemContainer.addOrDropItemStack(store, playerRef, bobberContainer, bobberSlot, bobberStack.withQuantity(1));
            return false;
        }

        if (!ItemStack.isEmpty(previous)) {
            SimpleItemContainer.addOrDropItemStack(store, playerRef, bobberContainer, previous);
        }
        return true;
    }

    public static boolean clearSlot(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ItemContext rodContext,
        int slotIndex,
        @Nonnull ItemContainer returnContainer
    ) {
        ItemStack rodStack = rodContext.getItemStack();
        int slotCount = getSlotCount(rodStack.getItemId());
        if (slotIndex < 0 || slotIndex >= slotCount) {
            return false;
        }

        ItemStack[] slots = getEquippedSlots(rodStack);
        ItemStack previous = slots[slotIndex];
        if (ItemStack.isEmpty(previous)) {
            return true;
        }

        slots[slotIndex] = null;
        ItemStack updatedRod = writeSlots(rodStack, slots);
        ItemStackSlotTransaction rodReplace =
            rodContext.getContainer().replaceItemStackInSlot(rodContext.getSlot(), rodStack, updatedRod);
        if (!rodReplace.succeeded()) {
            return false;
        }

        SimpleItemContainer.addOrDropItemStack(store, playerRef, returnContainer, previous);
        return true;
    }

    @Nonnull
    private static ItemStack[] readStoredSlots(@Nonnull ItemStack rodStack) {
        EquippedBobbersMetadata metadata = rodStack.getFromMetadataOrNull(EquippedBobbersMetadata.KEYED_CODEC);
        return metadata == null ? ItemStack.EMPTY_ARRAY : metadata.getSlots();
    }
}
