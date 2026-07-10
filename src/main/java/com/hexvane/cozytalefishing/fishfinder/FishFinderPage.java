package com.hexvane.cozytalefishing.fishfinder;

import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.SpawnProbeService;
import com.hexvane.cozytalefishing.fish.WaterBodyType;
import com.hexvane.cozytalefishing.journal.CozyInteractiveCustomUIPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishFinderPage extends CozyInteractiveCustomUIPage<FishFinderPage.PageData> {
    private static final String FISH_LIST = "#FishList";
    private static final String ROW_TEMPLATE = "CozyTalesFishing/FishFinderRow.ui";

    private boolean templateAppended;
    @Nullable
    private final SpawnProbeService.PlayerProbeResult location;

    public FishFinderPage(@Nonnull PlayerRef playerRef, @Nullable SpawnProbeService.PlayerProbeResult location) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.location = location;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("CozyTalesFishing/FishFinderPage.ui");
            templateAppended = true;
        }

        commandBuilder.set("#PageTitle.TextSpans", Message.translation("server.cozytalefishing.fishfinder.title"));

        if (location == null) {
            commandBuilder.set("#Subtitle.Visible", false);
            commandBuilder.set("#FishListPanel.Visible", false);
            commandBuilder.set("#EmptyPanel.Visible", true);
            commandBuilder.set(
                "#EmptyMessage.TextSpans",
                Message.translation("server.cozytalefishing.fishfinder.empty_no_water")
            );
            return;
        }

        List<SpawnProbeService.SpeciesProbeEntry> eligible = location.probeResult().eligibleNow();

        commandBuilder.set("#Subtitle.Visible", true);
        commandBuilder.set(
            "#Subtitle.TextSpans",
            Message
                .translation("server.cozytalefishing.fishfinder.subtitle")
                .param("waterBody", waterBodyMessage(location.probeResult().spawnWaterBody()))
                .param("count", eligible.size())
        );

        if (eligible.isEmpty()) {
            commandBuilder.set("#FishListPanel.Visible", false);
            commandBuilder.set("#EmptyPanel.Visible", true);
            commandBuilder.set(
                "#EmptyMessage.TextSpans",
                Message.translation("server.cozytalefishing.fishfinder.empty_no_fish")
            );
            return;
        }

        commandBuilder.set("#EmptyPanel.Visible", false);
        commandBuilder.set("#FishListPanel.Visible", true);
        commandBuilder.clear(FISH_LIST);

        for (int i = 0; i < eligible.size(); i++) {
            FishSpeciesAsset species = eligible.get(i).species();
            String selector = FISH_LIST + "[" + i + "]";
            commandBuilder.append(FISH_LIST, ROW_TEMPLATE);

            String itemId = species.getItemId();
            if (itemId != null && !itemId.isBlank()) {
                ItemStack stack = new ItemStack(itemId);
                commandBuilder.set(selector + " #Icon.ItemId", itemId);
                commandBuilder.set(selector + " #Name.TextSpans", stack.getDisplayName());
            } else {
                commandBuilder.clear(selector + " #Icon");
                commandBuilder.set(selector + " #Name.TextSpans", Message.raw(species.getId()));
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // Read-only list; no interactions.
    }

    @Nonnull
    private static Message waterBodyMessage(@Nonnull WaterBodyType waterBody) {
        return Message.translation(
            "server.cozytalefishing.journal.filter." + waterBody.name().toLowerCase(Locale.ROOT)
        );
    }

    public static final class PageData {
        @Nonnull
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new).build();
    }
}
