package com.hexvane.cozytalefishing.fish;

import javax.annotation.Nonnull;

public final class FishSpeciesDisplayNames {
    private FishSpeciesDisplayNames() {}

    @Nonnull
    public static String resolve(@Nonnull FishSpeciesAsset species) {
        var item = com.hypixel.hytale.server.core.asset.type.item.config.Item.getAssetMap().getAsset(species.getItemId());
        if (item != null && item.getTranslationProperties() != null) {
            String nameKey = item.getTranslationProperties().getName();
            if (nameKey != null && !nameKey.isBlank()) {
                var i18n = com.hypixel.hytale.server.core.modules.i18n.I18nModule.get();
                if (i18n != null) {
                    String resolved = i18n.getMessage(com.hypixel.hytale.server.core.modules.i18n.I18nModule.DEFAULT_LANGUAGE, nameKey);
                    if (resolved != null && !resolved.isBlank()) {
                        return resolved;
                    }
                }
            }
        }
        return species.getItemId();
    }
}
