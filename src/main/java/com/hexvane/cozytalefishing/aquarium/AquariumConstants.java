package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AquariumConstants {
    public static final String AQUARIUM_SMALL = "CozyFishing_Aquarium_Small";
    public static final String AQUARIUM_2X1 = "CozyFishing_Aquarium_2x1";
    public static final String AQUARIUM_3X2X2 = "CozyFishing_Aquarium_3x2x2";

    private static final Map<String, AquariumSize> SIZE_BY_BLOCK_ID =
        Map.ofEntries(
            Map.entry(AQUARIUM_SMALL, AquariumSize.Small),
            Map.entry(AQUARIUM_2X1, AquariumSize.Wide2x1),
            Map.entry(AQUARIUM_3X2X2, AquariumSize.Tall3x2x2)
        );

    private AquariumConstants() {}

    public static boolean isAquariumBlockId(@Nonnull String blockId) {
        return SIZE_BY_BLOCK_ID.containsKey(blockId);
    }

    @Nullable
    public static AquariumSize sizeForBlockId(@Nullable String blockId) {
        if (blockId == null) {
            return null;
        }
        return SIZE_BY_BLOCK_ID.get(blockId);
    }

    @Nonnull
    public static String blockIdForSize(@Nonnull AquariumSize size) {
        return switch (size) {
            case Small -> AQUARIUM_SMALL;
            case Wide2x1 -> AQUARIUM_2X1;
            case Tall3x2x2 -> AQUARIUM_3X2X2;
        };
    }
}
