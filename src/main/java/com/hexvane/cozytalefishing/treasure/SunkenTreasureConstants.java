package com.hexvane.cozytalefishing.treasure;

import javax.annotation.Nonnull;

public final class SunkenTreasureConstants {
    public static final String ITEM_ID = "CozyFishing_Sunken_Treasure";
    public static final String DROP_LIST_ID = "CozySunken_Treasure";

    private SunkenTreasureConstants() {}

    @Nonnull
    public static String itemId() {
        return ITEM_ID;
    }

    @Nonnull
    public static String dropListId() {
        return DROP_LIST_ID;
    }
}
