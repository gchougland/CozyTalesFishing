package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum FishShadowType {
    Tiny("NPC/FishShadow/FishShadow_Tiny.png", "CozyTalesFishing_FishShadow_Tiny"),
    Small("NPC/FishShadow/FishShadow_Small.png", "CozyTalesFishing_FishShadow_Small"),
    MediumSmall("NPC/FishShadow/FishShadow_MediumSmall.png", "CozyTalesFishing_FishShadow_MediumSmall"),
    Medium("NPC/FishShadow/FishShadow_Medium.png", "CozyTalesFishing_FishShadow_Medium"),
    Large("NPC/FishShadow/FishShadow_Large.png", "CozyTalesFishing_FishShadow_Large"),
    Thin("NPC/FishShadow/FishShadow_Thin.png", "CozyTalesFishing_FishShadow_Thin"),
    Fin("NPC/FishShadow/FishShadow_Fin.png", "CozyTalesFishing_FishShadow_Fin");

    private final String texturePath;
    private final String modelAssetId;

    FishShadowType(@Nonnull String texturePath, @Nonnull String modelAssetId) {
        this.texturePath = texturePath;
        this.modelAssetId = modelAssetId;
    }

    @Nonnull
    public String getTexturePath() {
        return texturePath;
    }

    @Nonnull
    public String getModelAssetId() {
        return modelAssetId;
    }

    @Nullable
    public static FishShadowType fromString(@Nonnull String value) {
        for (FishShadowType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isFishShadowModelId(@Nonnull String modelAssetId) {
        for (FishShadowType type : values()) {
            if (type.modelAssetId.equals(modelAssetId)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public static FishShadowType pickRandom(@Nonnull java.util.Random random) {
        FishShadowType[] types = values();
        return types[random.nextInt(types.length)];
    }
}
