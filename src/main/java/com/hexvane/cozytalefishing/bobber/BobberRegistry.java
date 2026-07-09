package com.hexvane.cozytalefishing.bobber;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BobberRegistry {
    public static final String TRASH_BOBBER_ID = "CozyFishing_Bobber_Trash";
    public static final String TREASURE_BOBBER_ID = "CozyFishing_Bobber_Treasure";
    public static final String QUALITY_BOBBER_ID = "CozyFishing_Bobber_Quality";
    public static final String TRAP_BOBBER_ID = "CozyFishing_Bobber_Trap";
    public static final String SPINNER_BOBBER_ID = "CozyFishing_Bobber_Spinner";
    public static final String DECORATED_SPINNER_BOBBER_ID = "CozyFishing_Bobber_Decorated_Spinner";

    public static final int DURABILITY_LOW = 20;
    public static final int DURABILITY_MEDIUM = 40;
    public static final int DURABILITY_HIGH = 60;

    private static final Map<String, BobberDefinition> BY_ITEM_ID =
        Map.of(
            TRASH_BOBBER_ID,
                new BobberDefinition(TRASH_BOBBER_ID, BobberType.TRASH, DURABILITY_HIGH, "CozyTalesFishing_Bobber_Trash"),
            TREASURE_BOBBER_ID,
                new BobberDefinition(TREASURE_BOBBER_ID, BobberType.TREASURE, DURABILITY_LOW, "CozyTalesFishing_Bobber_Treasure"),
            QUALITY_BOBBER_ID,
                new BobberDefinition(QUALITY_BOBBER_ID, BobberType.QUALITY, DURABILITY_MEDIUM, "CozyTalesFishing_Bobber_Quality"),
            TRAP_BOBBER_ID,
                new BobberDefinition(TRAP_BOBBER_ID, BobberType.TRAP, DURABILITY_MEDIUM, "CozyTalesFishing_Bobber_Trap"),
            SPINNER_BOBBER_ID,
                new BobberDefinition(SPINNER_BOBBER_ID, BobberType.SPINNER, DURABILITY_MEDIUM, "CozyTalesFishing_Bobber_Spinner"),
            DECORATED_SPINNER_BOBBER_ID,
                new BobberDefinition(
                    DECORATED_SPINNER_BOBBER_ID,
                    BobberType.DECORATED_SPINNER,
                    DURABILITY_MEDIUM,
                    "CozyTalesFishing_Bobber_Decorated_Spinner"
                )
        );

    private BobberRegistry() {}

    public static boolean isBobber(@Nullable String itemId) {
        return itemId != null && BY_ITEM_ID.containsKey(itemId);
    }

    @Nullable
    public static BobberDefinition get(@Nullable String itemId) {
        return itemId == null ? null : BY_ITEM_ID.get(itemId);
    }

    @Nonnull
    public static Map<String, BobberDefinition> all() {
        return BY_ITEM_ID;
    }

    public record BobberDefinition(
        @Nonnull String itemId,
        @Nonnull BobberType type,
        int maxDurability,
        @Nonnull String projectileModelId
    ) {}
}
