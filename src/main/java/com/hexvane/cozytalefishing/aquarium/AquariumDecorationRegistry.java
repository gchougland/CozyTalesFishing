package com.hexvane.cozytalefishing.aquarium;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AquariumDecorationRegistry {
    private static final Map<String, AquariumDecorationAsset> BY_ITEM_ID = new HashMap<>();
    private static volatile boolean initialized;

    private AquariumDecorationRegistry() {}

    public static void rebuild() {
        BY_ITEM_ID.clear();
        for (AquariumDecorationAsset asset : AquariumDecorationAsset.getAssetMap().getAssetMap().values()) {
            if (asset == null) {
                continue;
            }
            String itemId = asset.getItemId();
            if (itemId != null && !itemId.isBlank()) {
                BY_ITEM_ID.put(itemId, asset);
            }
        }
        initialized = true;
    }

    @Nullable
    public static AquariumDecorationAsset getByItemId(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        if (!initialized) {
            rebuild();
        }
        return BY_ITEM_ID.get(itemId);
    }
}
