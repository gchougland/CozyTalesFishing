package com.hexvane.cozytalefishing.journal;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishingJournalUi {
    private static final Value<String> DEFAULT_TEXT_TOOLTIP_STYLE = Value.ref("Common.ui", "DefaultTextTooltipStyle");

    private FishingJournalUi() {}

    public static void applyStaticLabels(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.set("#JournalTitle.TextSpans", Message.translation("server.cozytalefishing.journal.title"));
        commandBuilder.set("#FilterTitle.TextSpans", Message.translation("server.cozytalefishing.journal.filter_title"));
        commandBuilder.set("#GridTitle.TextSpans", Message.translation("server.cozytalefishing.journal.grid_title"));
        commandBuilder.set("#DetailTitle.TextSpans", Message.translation("server.cozytalefishing.journal.detail_title"));
        commandBuilder.set("#HabitatHeading.TextSpans", Message.translation("server.cozytalefishing.journal.habitat_heading"));
        commandBuilder.set("#ConditionsHeading.TextSpans", Message.translation("server.cozytalefishing.journal.conditions_heading"));
        commandBuilder.set("#RecordHeading.TextSpans", Message.translation("server.cozytalefishing.journal.record_heading"));
        commandBuilder.set("#UndiscoveredHint.TextSpans", Message.translation("server.cozytalefishing.journal.undiscovered_hint"));
        commandBuilder.set("#HintedHint.TextSpans", Message.translation("server.cozytalefishing.journal.hinted_hint"));
    }

    public static boolean isKnownItemId(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        return Item.getAssetMap().getAsset(itemId.trim()) != null;
    }

    public static void setFishIcon(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String assetImageSelector,
        @Nonnull String itemId
    ) {
        if (!isKnownItemId(itemId)) {
            return;
        }
        Item item = Item.getAssetMap().getAsset(itemId.trim());
        commandBuilder.set(assetImageSelector + ".AssetPath", ItemAssetImagePath.forItem(item, itemId));
    }

    public static void setGridCellTooltip(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String buttonSelector,
        @Nonnull String displayName,
        @Nonnull JournalEntryState state
    ) {
        commandBuilder.set(buttonSelector + ".TextTooltipStyle", DEFAULT_TEXT_TOOLTIP_STYLE);
        switch (state) {
            case DISCOVERED -> commandBuilder.set(buttonSelector + ".TooltipTextSpans", Message.raw(displayName));
            case HINTED -> commandBuilder.set(
                buttonSelector + ".TooltipTextSpans",
                Message.translation("server.cozytalefishing.journal.hinted_tooltip")
            );
            case UNDISCOVERED -> commandBuilder.set(
                buttonSelector + ".TooltipTextSpans",
                Message.translation("server.cozytalefishing.journal.undiscovered_tooltip")
            );
        }
    }
}
