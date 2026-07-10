package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves item models for aquarium fish and decoration display props. */
public final class AquariumDisplayModelResolver {
    private AquariumDisplayModelResolver() {}

    @Nullable
    public static ModelAsset resolveModelAsset(@Nonnull Item item) {
        String modelKey = item.getModel();
        if (modelKey != null && !modelKey.isBlank()) {
            ModelAsset direct = ModelAsset.getAssetMap().getAsset(modelKey);
            if (direct != null) {
                return direct;
            }
            for (ModelAsset candidate : ModelAsset.getAssetMap().getAssetMap().values()) {
                if (candidate != null && modelKey.equals(candidate.getModel())) {
                    return candidate;
                }
            }
        }

        String itemId = item.getId();
        if (itemId.startsWith("Fish_") && itemId.endsWith("_Item")) {
            String derivedId = itemId.substring("Fish_".length(), itemId.length() - "_Item".length());
            ModelAsset derived = ModelAsset.getAssetMap().getAsset(derivedId);
            if (derived != null) {
                return derived;
            }
        }
        return null;
    }

    public static boolean usesBlockEntityVisual(@Nonnull Item item) {
        return resolveModelAsset(item) == null && item.hasBlockType();
    }

    @Nullable
    public static String blockTypeKey(@Nonnull Item item) {
        if (!item.hasBlockType()) {
            return null;
        }
        String blockId = item.getBlockId();
        return blockId != null && !blockId.isBlank() ? blockId : item.getId();
    }
}
