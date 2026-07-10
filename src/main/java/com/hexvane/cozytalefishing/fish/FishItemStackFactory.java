package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Builds fish item stacks whose vanilla quality tier matches species rarity. */
public final class FishItemStackFactory {
    private FishItemStackFactory() {}

    @Nonnull
    public static ItemStack forSpecies(@Nonnull FishSpeciesAsset species) {
        return forItemId(species.getItemId(), species.getRarity());
    }

    @Nonnull
    public static ItemStack forItemId(@Nonnull String itemId) {
        FishSpeciesAsset species = FishSpeciesRegistry.getSpeciesByItemId(itemId);
        if (species != null) {
            return forSpecies(species);
        }
        return new ItemStack(itemId, 1);
    }

    @Nonnull
    public static ItemStack forItemId(@Nonnull String itemId, @Nonnull FishRarity rarity) {
        ItemStack stack = new ItemStack(itemId, 1);
        Item item = stack.getItem();
        if (item == null) {
            return stack;
        }

        String targetQuality = rarity.name();
        String baseQuality = qualityId(item);
        if (baseQuality != null && baseQuality.equalsIgnoreCase(targetQuality)) {
            return stack;
        }

        if (item.getItemIdForState(targetQuality) != null) {
            return stack.withState(targetQuality);
        }

        return stack;
    }

    @Nullable
    private static String qualityId(@Nonnull Item item) {
        ItemQuality quality = ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
        return quality != null ? quality.getId() : null;
    }
}
