package com.hexvane.cozytalefishing.journal;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves UI {@code AssetImage.AssetPath} from item data without using {@code ItemGrid} slots. */
public final class ItemAssetImagePath {
    private ItemAssetImagePath() {}

    @Nonnull
    public static String forItem(@Nullable Item item, @Nonnull String itemId) {
        if (item != null) {
            String icon = item.getIcon();
            if (icon != null && !icon.isBlank()) {
                return icon.trim();
            }
        }
        return "Icons/ItemsGenerated/" + itemId.trim() + ".png";
    }
}
