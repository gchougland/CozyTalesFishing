package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AquariumLayoutRegistry {
    private static final Map<AquariumSize, AquariumLayoutAsset> BY_SIZE = new EnumMap<>(AquariumSize.class);
    private static volatile boolean initialized;

    private AquariumLayoutRegistry() {}

    public static void rebuild() {
        BY_SIZE.clear();
        for (AquariumLayoutAsset asset : AquariumLayoutAsset.getAssetMap().getAssetMap().values()) {
            if (asset == null) {
                continue;
            }
            AquariumSize size = asset.getAquariumSize();
            if (size != null) {
                BY_SIZE.put(size, asset);
            }
        }
        initialized = true;
    }

    @Nonnull
    public static AquariumLayoutAsset getLayout(@Nonnull AquariumSize size) {
        if (!initialized) {
            rebuild();
        }
        AquariumLayoutAsset layout = BY_SIZE.get(size);
        return layout != null ? layout : fallbackLayout(size);
    }

    @Nonnull
    private static AquariumLayoutAsset fallbackLayout(@Nonnull AquariumSize size) {
        AquariumLayoutAsset fallback = new AquariumLayoutAsset();
        fallback.id =
            switch (size) {
                case Small -> "Small";
                case Wide2x1 -> "Wide2x1";
                case Tall3x2x2 -> "Tall3x2x2";
            };
        return fallback;
    }
}
