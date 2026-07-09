package com.hexvane.cozytalefishing.journal;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import com.hexvane.cozytalefishing.fish.FishCatchRecordSync;
import com.hexvane.cozytalefishing.fish.FishScoreCalculator;
import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.FishSpeciesDisplayNames;
import com.hexvane.cozytalefishing.fish.FishSpeciesMetadataFormatter;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hexvane.cozytalefishing.fish.WaterBodyType;
import com.hexvane.cozytalefishing.leaderboard.FishingLeaderboardService;
import com.hexvane.cozytalefishing.leaderboard.LeaderboardSnapshot;
import com.hexvane.cozytalefishing.leaderboard.RankedLeaderboardEntry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishingJournalPage extends CozyInteractiveCustomUIPage<FishingJournalPage.PageData> {
    private static final String FILTER_ROWS = "#FilterScroll #FilterRows";
    private static final String FISH_GRID = "#FishGridScroll #FishGrid";
    private static final String DETAIL_ICON = "#DetailIconFrame #DetailIconInner";
    private static final String SECTION_TAB_BUTTONS = "#SectionTabButtons";
    private static final String LEADERBOARD_TAB_BUTTONS = "#LeaderboardTabButtons";
    private static final String LEADERBOARD_ROWS = "#LeaderboardScroll #LeaderboardRows";
    private static final String SECTION_TAB_TEMPLATE = "CozyTalesFishing/FishingJournalSectionTab.ui";
    private static final String LEADERBOARD_TAB_TEMPLATE = "CozyTalesFishing/FishingJournalLeaderboardTab.ui";
    private static final String LEADERBOARD_ROW_TEMPLATE = "CozyTalesFishing/FishingJournalLeaderboardRow.ui";
    private static final WaterBodyType[] FILTER_TYPES = WaterBodyType.values();
    private static final NumberFormat SCORE_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private static final Value<String> NORMAL_ROW_STYLE =
        Value.ref("CozyTalesFishing/FishingJournalLeaderboardRow.ui", "NormalRowStyle");
    private static final Value<String> SELECTED_ROW_STYLE =
        Value.ref("CozyTalesFishing/FishingJournalLeaderboardRow.ui", "SelectedRowStyle");

    private boolean templateAppended;
    @Nonnull
    private final Set<WaterBodyType> activeWaterFilters = EnumSet.allOf(WaterBodyType.class);
    @Nonnull
    private JournalSection activeSection = JournalSection.SPECIES;
    @Nonnull
    private LeaderboardMetric activeLeaderboardMetric = LeaderboardMetric.TOTAL;
    @Nullable
    private String selectedSpeciesId;

    public FishingJournalPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("CozyTalesFishing/FishingJournalPage.ui");
            templateAppended = true;
        }

        FishCatchRecordSync.scheduleDisplayNameSync(ref, store, playerRef);
        FishingJournalUi.applyStaticLabels(commandBuilder);

        bindSectionTabs(commandBuilder, eventBuilder);

        boolean showSpecies = activeSection == JournalSection.SPECIES;
        boolean showLeaderboard = activeSection == JournalSection.LEADERBOARD;
        commandBuilder.set("#SpeciesPanel.Visible", showSpecies);
        commandBuilder.set("#LeaderboardPanel.Visible", showLeaderboard);

        if (showSpecies) {
            buildSpeciesPanel(commandBuilder, eventBuilder, ref, store);
        }
        if (showLeaderboard) {
            bindLeaderboard(commandBuilder, eventBuilder, ref, store);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if ("ChangeSection".equals(data.action) && data.section != null) {
            JournalSection section = JournalSection.fromString(data.section);
            if (section != null) {
                activeSection = section;
            }
        } else if ("ChangeLeaderboardTab".equals(data.action) && data.leaderboardMetric != null) {
            LeaderboardMetric metric = LeaderboardMetric.fromString(data.leaderboardMetric);
            if (metric != null) {
                activeLeaderboardMetric = metric;
            }
        } else if ("WaterFilterToggle".equals(data.action)) {
            applyWaterFilterToggle(data);
        } else if ("SelectFish".equals(data.action) && data.speciesId != null && !data.speciesId.isBlank()) {
            selectedSpeciesId = data.speciesId;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        build(ref, commandBuilder, eventBuilder, store);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void buildSpeciesPanel(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
        List<FishSpeciesAsset> allSpecies = FishSpeciesRegistry.getJournalSpecies();
        int discoveredCount = records != null ? records.getDiscoveredCount() : 0;

        commandBuilder.set(
            "#DiscoveredCount.TextSpans",
            Message.translation("server.cozytalefishing.journal.discovered_count")
                .param("current", discoveredCount)
                .param("total", allSpecies.size())
        );
        commandBuilder.set(
            "#TotalCaughtCount.TextSpans",
            Message.translation("server.cozytalefishing.journal.total_caught_count")
                .param("count", records != null ? records.getTotalCatchCount() : 0)
        );

        bindWaterFilters(commandBuilder, eventBuilder);

        List<FishSpeciesAsset> visibleSpecies = filterAndSortSpecies(allSpecies, records);
        ensureSelection(visibleSpecies);

        commandBuilder.clear(FISH_GRID);
        for (int i = 0; i < visibleSpecies.size(); i++) {
            FishSpeciesAsset species = visibleSpecies.get(i);
            String cell = FISH_GRID + "[" + i + "]";
            JournalEntryState state = JournalEntryState.fromRecords(records, species.getId());
            boolean selected = species.getId().equals(selectedSpeciesId);
            String button = cell + " #SelectButton";
            String icon = button + " #IconFrame #IconInner #FishIcon";

            commandBuilder.append(FISH_GRID, "CozyTalesFishing/FishingJournalFishCell.ui");
            commandBuilder.set(cell + " #SelectHilite.Visible", selected);
            FishingJournalUi.setFishIcon(commandBuilder, icon, species.getItemId());
            applyGridEntryState(commandBuilder, button, state);
            FishingJournalUi.setGridCellTooltip(
                commandBuilder,
                button,
                FishSpeciesDisplayNames.resolve(species),
                state
            );

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                button,
                new EventData().append("Action", "SelectFish").append("SpeciesId", species.getId()),
                false
            );
        }

        populateDetailPanel(commandBuilder, records, allSpecies);
    }

    private void bindSectionTabs(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear(SECTION_TAB_BUTTONS);
        JournalSection[] sections = JournalSection.values();
        for (int i = 0; i < sections.length; i++) {
            JournalSection section = sections[i];
            commandBuilder.append(SECTION_TAB_BUTTONS, SECTION_TAB_TEMPLATE);
            String tab = SECTION_TAB_BUTTONS + "[" + i + "]";
            commandBuilder.set(
                tab + ".TextSpans",
                Message.translation("server.cozytalefishing.journal.section." + section.name().toLowerCase(Locale.ROOT))
            );
            commandBuilder.set(tab + ".Disabled", activeSection == section);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                tab,
                new EventData().append("Action", "ChangeSection").append("Section", section.name()),
                false
            );
        }
    }

    private void bindLeaderboard(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        bindLeaderboardMetricTabs(commandBuilder, eventBuilder);
        commandBuilder.set(
            "#LeaderboardColScore.TextSpans",
            activeLeaderboardMetric == LeaderboardMetric.BEST_CATCH
                ? Message.translation("server.cozytalefishing.journal.leaderboard.col.catch")
                : Message.translation("server.cozytalefishing.journal.leaderboard.col.score")
        );

        LeaderboardSnapshot snapshot = FishingLeaderboardService.getCachedSnapshot();
        boolean loading = FishingLeaderboardService.isLoading();
        if (snapshot == null && !loading) {
            requestLeaderboardRefresh(ref, store);
        }

        List<RankedLeaderboardEntry> entries = switch (activeLeaderboardMetric) {
            case TOTAL -> snapshot != null ? snapshot.getTotalScoreEntries() : List.of();
            case BEST_CATCH -> snapshot != null ? snapshot.getBestCatchEntries() : List.of();
            case TOTAL_CAUGHT -> snapshot != null ? snapshot.getTotalCaughtEntries() : List.of();
        };
        entries = ensureViewerListed(entries, ref, store);

        boolean showLoading = loading && snapshot == null;
        boolean showEmpty = !showLoading && entries.isEmpty();

        commandBuilder.set("#LeaderboardLoading.Visible", showLoading);
        commandBuilder.set("#LeaderboardScroll.Visible", !showLoading && !showEmpty);
        commandBuilder.set("#LeaderboardEmpty.Visible", showEmpty);

        if (showLoading) {
            commandBuilder.set(
                "#LeaderboardLoading.TextSpans",
                Message.translation("server.cozytalefishing.journal.leaderboard.loading")
            );
            commandBuilder.clear(LEADERBOARD_ROWS);
        } else if (showEmpty) {
            commandBuilder.set(
                "#LeaderboardEmpty.TextSpans",
                Message.translation("server.cozytalefishing.journal.leaderboard.empty")
            );
            commandBuilder.clear(LEADERBOARD_ROWS);
        } else {
            bindLeaderboardRows(commandBuilder, entries);
        }

        bindViewerRankPanel(commandBuilder, ref, store, entries);
    }

    private void bindLeaderboardMetricTabs(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear(LEADERBOARD_TAB_BUTTONS);
        LeaderboardMetric[] metrics = LeaderboardMetric.values();
        for (int i = 0; i < metrics.length; i++) {
            LeaderboardMetric metric = metrics[i];
            commandBuilder.append(LEADERBOARD_TAB_BUTTONS, LEADERBOARD_TAB_TEMPLATE);
            String tab = LEADERBOARD_TAB_BUTTONS + "[" + i + "]";
            commandBuilder.set(
                tab + ".TextSpans",
                Message.translation("server.cozytalefishing.journal.leaderboard.tab." + metric.langKey())
            );
            commandBuilder.set(tab + ".Disabled", activeLeaderboardMetric == metric);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                tab,
                new EventData().append("Action", "ChangeLeaderboardTab").append("LeaderboardMetric", metric.name()),
                false
            );
        }
    }

    @Nonnull
    private List<RankedLeaderboardEntry> ensureViewerListed(
        @Nonnull List<RankedLeaderboardEntry> entries,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        UUID viewerUuid = playerRef.getUuid();
        for (RankedLeaderboardEntry entry : entries) {
            if (entry.playerUuid().equals(viewerUuid)) {
                return entries;
            }
        }

        FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
        int viewerScore = switch (activeLeaderboardMetric) {
            case TOTAL -> FishScoreCalculator.totalScore(records);
            case BEST_CATCH -> FishScoreCalculator.bestCatchScore(records);
            case TOTAL_CAUGHT -> records != null ? records.getTotalCatchCount() : 0;
        };
        if (viewerScore <= 0) {
            return entries;
        }

        String displayName = records != null ? records.getLeaderboardDisplayName() : "";
        if (displayName.isBlank()) {
            displayName = playerRef.getUsername();
        }
        String bestCatchSpeciesId = null;
        float bestCatchSizeCm = 0.0f;
        FishScoreCalculator.BestCatch bestCatch = FishScoreCalculator.findBestCatch(records);
        if (bestCatch != null) {
            bestCatchSpeciesId = bestCatch.speciesId();
            bestCatchSizeCm = bestCatch.sizeCm();
        }
        List<RankedLeaderboardEntry> withViewer = new ArrayList<>(entries);
        withViewer.add(
            new RankedLeaderboardEntry(viewerUuid, displayName, viewerScore, 1, bestCatchSpeciesId, bestCatchSizeCm)
        );
        withViewer.sort(Comparator.comparingInt(RankedLeaderboardEntry::score).reversed());

        List<RankedLeaderboardEntry> ranked = new ArrayList<>();
        int rank = 0;
        int position = 0;
        int lastScore = -1;
        for (RankedLeaderboardEntry entry : withViewer) {
            position++;
            if (entry.score() != lastScore) {
                rank = position;
                lastScore = entry.score();
            }
            ranked.add(
                new RankedLeaderboardEntry(
                    entry.playerUuid(),
                    entry.displayName(),
                    entry.score(),
                    rank,
                    entry.bestCatchSpeciesId(),
                    entry.bestCatchSizeCm()
                )
            );
        }
        return ranked;
    }

    private void bindLeaderboardRows(@Nonnull UICommandBuilder commandBuilder, @Nonnull List<RankedLeaderboardEntry> entries) {
        commandBuilder.clear(LEADERBOARD_ROWS);
        UUID viewerUuid = playerRef.getUuid();
        for (int i = 0; i < entries.size(); i++) {
            RankedLeaderboardEntry entry = entries.get(i);
            String row = LEADERBOARD_ROWS + "[" + i + "]";
            commandBuilder.append(LEADERBOARD_ROWS, LEADERBOARD_ROW_TEMPLATE);
            boolean isViewer = entry.playerUuid().equals(viewerUuid);
            commandBuilder.set(row + ".Style", isViewer ? SELECTED_ROW_STYLE : NORMAL_ROW_STYLE);
            commandBuilder.set(row + " #RankLabel.TextSpans", Message.raw(formatRank(entry.rank())));
            commandBuilder.set(row + " #PlayerName.TextSpans", displayNameMessage(entry.displayName()));
            if (activeLeaderboardMetric == LeaderboardMetric.BEST_CATCH) {
                commandBuilder.set(row + " #ScoreLabel.TextSpans", formatBestCatchMessage(entry.bestCatchSpeciesId(), entry.bestCatchSizeCm()));
            } else {
                commandBuilder.set(row + " #ScoreLabel.TextSpans", Message.raw(formatScore(entry.score())));
            }
            applyRankAccent(commandBuilder, row + " #RankLabel", entry.rank());
        }
    }

    private void bindViewerRankPanel(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull List<RankedLeaderboardEntry> entries
    ) {
        UUID viewerUuid = playerRef.getUuid();
        FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
        int viewerScore = switch (activeLeaderboardMetric) {
            case TOTAL -> FishScoreCalculator.totalScore(records);
            case BEST_CATCH -> FishScoreCalculator.bestCatchScore(records);
            case TOTAL_CAUGHT -> records != null ? records.getTotalCatchCount() : 0;
        };

        RankedLeaderboardEntry viewerEntry = null;
        for (RankedLeaderboardEntry entry : entries) {
            if (entry.playerUuid().equals(viewerUuid)) {
                viewerEntry = entry;
                break;
            }
        }

        if (viewerEntry != null) {
            commandBuilder.set(
                "#ViewerRankLabel.TextSpans",
                Message.translation("server.cozytalefishing.journal.leaderboard.your_rank")
                    .param("rank", viewerEntry.rank())
            );
            bindViewerScoreLabel(commandBuilder, viewerEntry, records);
        } else if (viewerScore > 0) {
            int displayRank = entries.isEmpty() ? 1 : 0;
            if (displayRank > 0) {
                commandBuilder.set(
                    "#ViewerRankLabel.TextSpans",
                    Message.translation("server.cozytalefishing.journal.leaderboard.your_rank")
                        .param("rank", displayRank)
                );
            } else {
                commandBuilder.set(
                    "#ViewerRankLabel.TextSpans",
                    Message.translation("server.cozytalefishing.journal.leaderboard.your_rank_unranked")
                );
            }
            bindViewerScoreLabel(commandBuilder, viewerScore, records);
        } else {
            commandBuilder.set(
                "#ViewerRankLabel.TextSpans",
                Message.translation("server.cozytalefishing.journal.leaderboard.your_rank_none")
            );
            commandBuilder.set(
                "#ViewerScoreLabel.TextSpans",
                Message.translation("server.cozytalefishing.journal.leaderboard.your_score")
                    .param("score", formatScore(0))
            );
        }
    }

    private void bindViewerScoreLabel(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull RankedLeaderboardEntry viewerEntry,
        @Nullable FishCatchRecordComponent records
    ) {
        if (activeLeaderboardMetric == LeaderboardMetric.BEST_CATCH) {
            commandBuilder.set(
                "#ViewerScoreLabel.TextSpans",
                formatBestCatchSummaryMessage(viewerEntry.bestCatchSpeciesId(), viewerEntry.bestCatchSizeCm(), records)
            );
            return;
        }
        commandBuilder.set(
            "#ViewerScoreLabel.TextSpans",
            Message.translation("server.cozytalefishing.journal.leaderboard.your_score")
                .param("score", formatScore(viewerEntry.score()))
        );
    }

    private void bindViewerScoreLabel(
        @Nonnull UICommandBuilder commandBuilder,
        int viewerScore,
        @Nullable FishCatchRecordComponent records
    ) {
        if (activeLeaderboardMetric == LeaderboardMetric.BEST_CATCH) {
            FishScoreCalculator.BestCatch bestCatch = FishScoreCalculator.findBestCatch(records);
            commandBuilder.set(
                "#ViewerScoreLabel.TextSpans",
                formatBestCatchSummaryMessage(
                    bestCatch != null ? bestCatch.speciesId() : null,
                    bestCatch != null ? bestCatch.sizeCm() : 0.0f,
                    records
                )
            );
            return;
        }
        commandBuilder.set(
            "#ViewerScoreLabel.TextSpans",
            Message.translation("server.cozytalefishing.journal.leaderboard.your_score")
                .param("score", formatScore(viewerScore))
        );
    }

    @Nonnull
    private Message formatBestCatchMessage(@Nullable String speciesId, float sizeCm) {
        if (speciesId == null || speciesId.isBlank() || sizeCm <= 0.0f) {
            return Message.raw("—");
        }
        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(speciesId);
        if (species == null) {
            return Message.raw(speciesId + " (" + String.format(Locale.US, "%.1f", sizeCm) + " cm)");
        }
        return Message.translation("server.cozytalefishing.journal.leaderboard.best_catch_value")
            .param("fish", FishSpeciesDisplayNames.resolve(species))
            .param("size", String.format(Locale.US, "%.1f", sizeCm));
    }

    @Nonnull
    private Message formatBestCatchSummaryMessage(
        @Nullable String speciesId,
        float sizeCm,
        @Nullable FishCatchRecordComponent records
    ) {
        if (speciesId == null || speciesId.isBlank() || sizeCm <= 0.0f) {
            FishScoreCalculator.BestCatch bestCatch = FishScoreCalculator.findBestCatch(records);
            if (bestCatch != null) {
                speciesId = bestCatch.speciesId();
                sizeCm = bestCatch.sizeCm();
            }
        }
        if (speciesId == null || speciesId.isBlank() || sizeCm <= 0.0f) {
            return Message.translation("server.cozytalefishing.journal.leaderboard.your_best_catch_none");
        }
        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(speciesId);
        if (species == null) {
            return Message.translation("server.cozytalefishing.journal.leaderboard.your_best_catch")
                .param("fish", speciesId)
                .param("size", String.format(Locale.US, "%.1f", sizeCm));
        }
        return Message.translation("server.cozytalefishing.journal.leaderboard.your_best_catch")
            .param("fish", FishSpeciesDisplayNames.resolve(species))
            .param("size", String.format(Locale.US, "%.1f", sizeCm));
    }

    private void requestLeaderboardRefresh(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        FishingLeaderboardService.requestRebuild(
            world,
            () ->
                world.execute(
                    () -> {
                        if (isDismissed() || !ref.isValid()) {
                            return;
                        }
                        UICommandBuilder commandBuilder = new UICommandBuilder();
                        UIEventBuilder eventBuilder = new UIEventBuilder();
                        build(ref, commandBuilder, eventBuilder, store);
                        sendUpdate(commandBuilder, eventBuilder, false);
                    }
                )
        );
    }

    private static void applyRankAccent(@Nonnull UICommandBuilder commandBuilder, @Nonnull String selector, int rank) {
        if (rank == 1) {
            commandBuilder.set(selector + ".Style.TextColor", "#e8c060");
        } else if (rank == 2) {
            commandBuilder.set(selector + ".Style.TextColor", "#c0c8d8");
        } else if (rank == 3) {
            commandBuilder.set(selector + ".Style.TextColor", "#c89868");
        }
    }

    @Nonnull
    private static String formatRank(int rank) {
        return "#" + rank;
    }

    @Nonnull
    private static String formatScore(int score) {
        return SCORE_FORMAT.format(score);
    }

    @Nonnull
    private static Message displayNameMessage(@Nullable String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            return Message.raw(displayName);
        }
        return Message.translation("server.cozytalefishing.journal.leaderboard.unknown_angler");
    }

    private void bindWaterFilters(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear(FILTER_ROWS);
        for (int i = 0; i < FILTER_TYPES.length; i++) {
            WaterBodyType bodyType = FILTER_TYPES[i];
            commandBuilder.append(FILTER_ROWS, "CozyTalesFishing/FishingJournalFilterRow.ui");
            String row = FILTER_ROWS + "[" + i + "]";
            commandBuilder.set(
                row + " #FilterLabel.TextSpans",
                Message.translation("server.cozytalefishing.journal.filter." + bodyType.name().toLowerCase(Locale.ROOT))
            );
            commandBuilder.set(row + " #CheckBox.Value", activeWaterFilters.contains(bodyType));
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                row + " #CheckBox",
                new EventData()
                    .append("Action", "WaterFilterToggle")
                    .append("WaterBodyType", bodyType.name())
                    .append("@Checked", row + " #CheckBox.Value"),
                false
            );
        }
    }

    private void applyWaterFilterToggle(@Nonnull PageData data) {
        if (data.waterBodyType == null || data.checked == null) {
            return;
        }
        WaterBodyType bodyType = WaterBodyType.fromString(data.waterBodyType);
        if (bodyType == null) {
            return;
        }
        if (data.checked) {
            activeWaterFilters.add(bodyType);
        } else {
            activeWaterFilters.remove(bodyType);
        }
    }

    @Nonnull
    private List<FishSpeciesAsset> filterAndSortSpecies(
        @Nonnull List<FishSpeciesAsset> allSpecies,
        @Nullable FishCatchRecordComponent records
    ) {
        List<FishSpeciesAsset> visible = new ArrayList<>();
        for (FishSpeciesAsset species : allSpecies) {
            if (matchesActiveFilters(species)) {
                visible.add(species);
            }
        }
        visible.sort(
            Comparator.comparingInt(
                    (FishSpeciesAsset species) ->
                        JournalEntryState.fromRecords(records, species.getId()).journalSortOrder()
                )
                .thenComparing(FishSpeciesDisplayNames::resolve, String.CASE_INSENSITIVE_ORDER)
        );
        return visible;
    }

    private boolean matchesActiveFilters(@Nonnull FishSpeciesAsset species) {
        if (activeWaterFilters.isEmpty()) {
            return false;
        }
        for (WaterBodyType bodyType : species.getWaterBodyTypes()) {
            if (activeWaterFilters.contains(bodyType)) {
                return true;
            }
        }
        return false;
    }

    private void ensureSelection(@Nonnull List<FishSpeciesAsset> visibleSpecies) {
        if (selectedSpeciesId != null) {
            for (FishSpeciesAsset species : visibleSpecies) {
                if (species.getId().equals(selectedSpeciesId)) {
                    return;
                }
            }
        }
        selectedSpeciesId = visibleSpecies.isEmpty() ? null : visibleSpecies.get(0).getId();
    }

    private void populateDetailPanel(
        @Nonnull UICommandBuilder commandBuilder,
        @Nullable FishCatchRecordComponent records,
        @Nonnull List<FishSpeciesAsset> allSpecies
    ) {
        FishSpeciesAsset species = findSpecies(allSpecies, selectedSpeciesId);
        if (species == null) {
            commandBuilder.set("#FishName.TextSpans", Message.translation("server.cozytalefishing.journal.no_selection"));
            commandBuilder.set("#DetailInfoScroll.Visible", false);
            commandBuilder.set("#UndiscoveredHint.Visible", false);
            commandBuilder.set("#HintedHint.Visible", false);
            commandBuilder.set(DETAIL_ICON + ".Visible", false);
            return;
        }

        JournalEntryState state = JournalEntryState.fromRecords(records, species.getId());
        commandBuilder.set(DETAIL_ICON + ".Visible", true);
        FishingJournalUi.setFishIcon(commandBuilder, DETAIL_ICON + " #DetailFishIcon", species.getItemId());
        applyDetailEntryState(commandBuilder, state);

        switch (state) {
            case DISCOVERED -> {
                commandBuilder.set("#FishName.TextSpans", Message.raw(FishSpeciesDisplayNames.resolve(species)));
                populateHabitatAndConditions(commandBuilder, species);
                populatePersonalBest(commandBuilder, records, species);
            }
            case HINTED -> {
                commandBuilder.set("#FishName.TextSpans", Message.translation("server.cozytalefishing.journal.undiscovered_name"));
                populateHabitatAndConditions(commandBuilder, species);
                commandBuilder.set("#CatchCountBody.Visible", false);
            }
            case UNDISCOVERED -> {
                commandBuilder.set(
                    "#FishName.TextSpans",
                    Message.translation("server.cozytalefishing.journal.undiscovered_name")
                );
                commandBuilder.set("#CatchCountBody.Visible", false);
                commandBuilder.set("#CatchScoreBody.Visible", false);
            }
        }
    }

    private static void applyGridEntryState(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String button,
        @Nonnull JournalEntryState state
    ) {
        boolean showDim = state != JournalEntryState.DISCOVERED;
        commandBuilder.set(button + " #IconFrame #DimOverlay.Visible", showDim);
        commandBuilder.set(button + " #IconFrame #LockIconWrap.Visible", state == JournalEntryState.UNDISCOVERED);
        commandBuilder.set(button + " #IconFrame #HintIconWrap.Visible", state == JournalEntryState.HINTED);
    }

    private static void applyDetailEntryState(@Nonnull UICommandBuilder commandBuilder, @Nonnull JournalEntryState state) {
        boolean showDim = state != JournalEntryState.DISCOVERED;
        commandBuilder.set(DETAIL_ICON + " #DetailDimOverlay.Visible", showDim);
        commandBuilder.set(DETAIL_ICON + " #DetailLockIconWrap.Visible", state == JournalEntryState.UNDISCOVERED);
        commandBuilder.set(DETAIL_ICON + " #DetailHintIconWrap.Visible", state == JournalEntryState.HINTED);
        commandBuilder.set("#DetailInfoScroll.Visible", state != JournalEntryState.UNDISCOVERED);
        commandBuilder.set("#UndiscoveredHint.Visible", state == JournalEntryState.UNDISCOVERED);
        commandBuilder.set("#HintedHint.Visible", state == JournalEntryState.HINTED);
        commandBuilder.set("#HabitatHeading.Visible", state != JournalEntryState.UNDISCOVERED);
        commandBuilder.set("#HabitatBody.Visible", state != JournalEntryState.UNDISCOVERED);
        commandBuilder.set("#ConditionsHeading.Visible", state != JournalEntryState.UNDISCOVERED);
        commandBuilder.set("#ConditionsBody.Visible", state != JournalEntryState.UNDISCOVERED);
        commandBuilder.set("#RecordHeading.Visible", state == JournalEntryState.DISCOVERED);
        commandBuilder.set("#RecordBody.Visible", state == JournalEntryState.DISCOVERED);
        commandBuilder.set("#CatchCountHeading.Visible", state == JournalEntryState.DISCOVERED);
        commandBuilder.set("#CatchCountBody.Visible", state == JournalEntryState.DISCOVERED);
        commandBuilder.set("#CatchScoreBody.Visible", state == JournalEntryState.DISCOVERED);
    }

    private static void populateHabitatAndConditions(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull FishSpeciesAsset species
    ) {
        commandBuilder.set(
            "#HabitatBody.TextSpans",
            Message.raw(FishSpeciesMetadataFormatter.formatHabitat(species))
        );

        commandBuilder.set(
            "#ConditionsBody.TextSpans",
            Message.raw(
                "Rarity: "
                    + FishSpeciesMetadataFormatter.formatRarity(species.getRarity())
                    + "\nShadow: "
                    + FishSpeciesMetadataFormatter.formatShadowType(species)
                    + "\n"
                    + FishSpeciesMetadataFormatter.formatSpawnRules(species)
                    + "\nSize: "
                    + FishSpeciesMetadataFormatter.formatSizeRange(species.getSizeRangeCm())
            )
        );
    }

    private static void populatePersonalBest(
        @Nonnull UICommandBuilder commandBuilder,
        @Nullable FishCatchRecordComponent records,
        @Nonnull FishSpeciesAsset species
    ) {
        float personalBest = records != null ? records.getLargestSizeCm(species.getId()) : 0.0f;
        int catchCount = records != null ? records.getEffectiveCatchCount(species.getId()) : 0;
        if (personalBest > 0.0f) {
            commandBuilder.set(
                "#RecordBody.TextSpans",
                Message.translation("server.cozytalefishing.journal.record_value")
                    .param("size", String.format(Locale.US, "%.1f", personalBest))
            );
            commandBuilder.set(
                "#CatchCountBody.TextSpans",
                Message.translation("server.cozytalefishing.journal.catch_count_value")
                    .param("count", catchCount)
            );
            commandBuilder.set(
                "#CatchScoreBody.TextSpans",
                Message.translation("server.cozytalefishing.journal.catch_score_value")
                    .param("score", formatScore(FishScoreCalculator.scoreCatch(species, personalBest)))
            );
        } else {
            commandBuilder.set("#RecordBody.TextSpans", Message.translation("server.cozytalefishing.journal.record_none"));
            commandBuilder.set("#CatchCountBody.TextSpans", Message.translation("server.cozytalefishing.journal.catch_count_none"));
            commandBuilder.set("#CatchScoreBody.TextSpans", Message.translation("server.cozytalefishing.journal.catch_score_none"));
        }
    }

    @Nullable
    private static FishSpeciesAsset findSpecies(@Nonnull List<FishSpeciesAsset> allSpecies, @Nullable String speciesId) {
        if (speciesId == null) {
            return null;
        }
        for (FishSpeciesAsset species : allSpecies) {
            if (species.getId().equals(speciesId)) {
                return species;
            }
        }
        return FishSpeciesRegistry.getSpecies(speciesId);
    }

    private enum JournalSection {
        SPECIES,
        LEADERBOARD;

        @Nullable
        static JournalSection fromString(@Nonnull String value) {
            for (JournalSection section : values()) {
                if (section.name().equalsIgnoreCase(value)) {
                    return section;
                }
            }
            return null;
        }
    }

    private enum LeaderboardMetric {
        TOTAL,
        BEST_CATCH,
        TOTAL_CAUGHT;

        @Nonnull
        String langKey() {
            return switch (this) {
                case TOTAL -> "total";
                case BEST_CATCH -> "best_catch";
                case TOTAL_CAUGHT -> "total_caught";
            };
        }

        @Nullable
        static LeaderboardMetric fromString(@Nonnull String value) {
            for (LeaderboardMetric metric : values()) {
                if (metric.name().equalsIgnoreCase(value)) {
                    return metric;
                }
            }
            return null;
        }
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .append(new KeyedCodec<>("SpeciesId", Codec.STRING), (d, v) -> d.speciesId = v, d -> d.speciesId)
            .add()
            .append(new KeyedCodec<>("WaterBodyType", Codec.STRING), (d, v) -> d.waterBodyType = v, d -> d.waterBodyType)
            .add()
            .append(new KeyedCodec<>("@Checked", Codec.BOOLEAN), (d, v) -> d.checked = v, d -> d.checked)
            .add()
            .append(new KeyedCodec<>("Section", Codec.STRING), (d, v) -> d.section = v, d -> d.section)
            .add()
            .append(new KeyedCodec<>("LeaderboardMetric", Codec.STRING), (d, v) -> d.leaderboardMetric = v, d -> d.leaderboardMetric)
            .add()
            .build();

        @Nullable
        private String action;
        @Nullable
        private String speciesId;
        @Nullable
        private String waterBodyType;
        @Nullable
        private Boolean checked;
        @Nullable
        private String section;
        @Nullable
        private String leaderboardMetric;
    }
}
