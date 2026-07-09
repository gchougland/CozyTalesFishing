package com.hexvane.cozytalefishing.journal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Locale;
import javax.annotation.Nonnull;

public final class FishCatchCelebrationPage extends CozyInteractiveCustomUIPage<FishCatchCelebrationPage.PageEventData> {
    public enum CelebrationType {
        NEW_SPECIES,
        NEW_RECORD
    }

    private static final String FISH_ICON = "#FishIconFrame #FishIconInner #FishIcon";

    private final float sizeCm;
    @Nonnull
    private final String itemId;
    @Nonnull
    private final String fishDisplayName;
    @Nonnull
    private final CelebrationType celebrationType;

    public FishCatchCelebrationPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull String itemId,
        @Nonnull String fishDisplayName,
        float sizeCm,
        @Nonnull CelebrationType celebrationType
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.itemId = itemId;
        this.fishDisplayName = fishDisplayName;
        this.sizeCm = sizeCm;
        this.celebrationType = celebrationType;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("CozyTalesFishing/FishCatchCelebrationPage.ui");

        String headlineKey =
            celebrationType == CelebrationType.NEW_SPECIES
                ? "server.cozytalefishing.catch_celebration.new_fish"
                : "server.cozytalefishing.catch_celebration.new_record";

        commandBuilder.set("#HeadlineLabel.TextSpans", Message.translation(headlineKey));
        commandBuilder.set("#FishName.TextSpans", Message.raw(fishDisplayName));
        commandBuilder.set(
            "#FishSize.TextSpans",
            Message
                .translation("server.cozytalefishing.catch_celebration.size")
                .param("size", String.format(Locale.US, "%.1f", sizeCm))
        );
        commandBuilder.set("#CloseButton.TextSpans", Message.translation("server.cozytalefishing.catch_celebration.close"));

        FishingJournalUi.setFishIcon(commandBuilder, FISH_ICON, itemId);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().setPage(ref, store, Page.None);
    }

    public static final class PageEventData {
        @Nonnull
        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new).build();
    }
}
