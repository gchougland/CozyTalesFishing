package com.hexvane.cozytalefishing.journal;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.FishSpeciesDisplayNames;
import com.hexvane.cozytalefishing.fish.FishSpeciesMetadataFormatter;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hexvane.cozytalefishing.fish.WaterBodyType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishingJournalPage extends CozyInteractiveCustomUIPage<FishingJournalPage.PageData> {
    private static final String FILTER_ROWS = "#FilterScroll #FilterRows";
    private static final String FISH_GRID = "#FishGridScroll #FishGrid";
    private static final String DETAIL_ICON = "#DetailIconFrame #DetailIconInner";
    private static final WaterBodyType[] FILTER_TYPES = WaterBodyType.values();

    private boolean templateAppended;
    @Nonnull
    private final Set<WaterBodyType> activeWaterFilters = EnumSet.allOf(WaterBodyType.class);
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
        FishingJournalUi.applyStaticLabels(commandBuilder);

        FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
        List<FishSpeciesAsset> allSpecies = FishSpeciesRegistry.getAllSpecies();
        int discoveredCount = records != null ? records.getDiscoveredCount() : 0;

        commandBuilder.set(
            "#DiscoveredCount.TextSpans",
            Message.translation("server.cozytalefishing.journal.discovered_count")
                .param("current", discoveredCount)
                .param("total", allSpecies.size())
        );

        bindWaterFilters(commandBuilder, eventBuilder);

        List<FishSpeciesAsset> visibleSpecies = filterAndSortSpecies(allSpecies);
        ensureSelection(visibleSpecies);

        commandBuilder.clear(FISH_GRID);
        for (int i = 0; i < visibleSpecies.size(); i++) {
            FishSpeciesAsset species = visibleSpecies.get(i);
            String cell = FISH_GRID + "[" + i + "]";
            boolean discovered = records != null && records.isDiscovered(species.getId());
            boolean selected = species.getId().equals(selectedSpeciesId);
            String button = cell + " #SelectButton";
            String icon = button + " #IconFrame #IconInner #FishIcon";

            commandBuilder.append(FISH_GRID, "CozyTalesFishing/FishingJournalFishCell.ui");
            commandBuilder.set(cell + " #SelectHilite.Visible", selected);
            FishingJournalUi.setFishIcon(commandBuilder, icon, species.getItemId());
            commandBuilder.set(button + " #IconFrame #DimOverlay.Visible", !discovered);
            commandBuilder.set(button + " #IconFrame #LockIconWrap.Visible", !discovered);
            FishingJournalUi.setGridCellTooltip(
                commandBuilder,
                button,
                FishSpeciesDisplayNames.resolve(species),
                discovered
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

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if ("WaterFilterToggle".equals(data.action)) {
            applyWaterFilterToggle(data);
        } else if ("SelectFish".equals(data.action) && data.speciesId != null && !data.speciesId.isBlank()) {
            selectedSpeciesId = data.speciesId;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        build(ref, commandBuilder, eventBuilder, store);
        sendUpdate(commandBuilder, eventBuilder, false);
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
    private List<FishSpeciesAsset> filterAndSortSpecies(@Nonnull List<FishSpeciesAsset> allSpecies) {
        List<FishSpeciesAsset> visible = new ArrayList<>();
        for (FishSpeciesAsset species : allSpecies) {
            if (matchesActiveFilters(species)) {
                visible.add(species);
            }
        }
        visible.sort(Comparator.comparing(FishSpeciesDisplayNames::resolve, String.CASE_INSENSITIVE_ORDER));
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
            commandBuilder.set(DETAIL_ICON + ".Visible", false);
            return;
        }

        boolean discovered = records != null && records.isDiscovered(species.getId());
        commandBuilder.set(DETAIL_ICON + ".Visible", true);
        FishingJournalUi.setFishIcon(commandBuilder, DETAIL_ICON + " #DetailFishIcon", species.getItemId());
        commandBuilder.set(DETAIL_ICON + " #DetailDimOverlay.Visible", !discovered);
        commandBuilder.set(DETAIL_ICON + " #DetailLockIconWrap.Visible", !discovered);
        commandBuilder.set("#DetailInfoScroll.Visible", discovered);
        commandBuilder.set("#UndiscoveredHint.Visible", !discovered);

        if (discovered) {
            commandBuilder.set("#FishName.TextSpans", Message.raw(FishSpeciesDisplayNames.resolve(species)));

            commandBuilder.set(
                "#HabitatBody.TextSpans",
                Message.raw(
                    FishSpeciesMetadataFormatter.formatWaterBodyTypes(species.getWaterBodyTypes())
                        + "\n"
                        + FishSpeciesMetadataFormatter.formatSpawnLocation(species)
                        + "\n"
                        + FishSpeciesMetadataFormatter.formatUnderground(species.isUndergroundOnly())
                )
            );

            float[] dayRange = species.getDayTimeRange();
            String timeOfDay = dayRange != null ? FishSpeciesMetadataFormatter.formatFloatRange(dayRange) : "—";
            commandBuilder.set(
                "#ConditionsBody.TextSpans",
                Message.raw(
                    "Rarity: "
                        + FishSpeciesMetadataFormatter.formatRarity(species.getRarity())
                        + "\nShadow: "
                        + FishSpeciesMetadataFormatter.formatShadowType(species.getShadowType())
                        + "\nTime: "
                        + timeOfDay
                        + "\nWeather: "
                        + FishSpeciesMetadataFormatter.formatWeather(species.getWeatherIds())
                        + "\nSize: "
                        + FishSpeciesMetadataFormatter.formatSizeRange(species.getSizeRangeCm())
                )
            );

            float personalBest = records != null ? records.getLargestSizeCm(species.getId()) : 0.0f;
            if (personalBest > 0.0f) {
                commandBuilder.set(
                    "#RecordBody.TextSpans",
                    Message.translation("server.cozytalefishing.journal.record_value")
                        .param("size", String.format(Locale.US, "%.1f", personalBest))
                );
            } else {
                commandBuilder.set("#RecordBody.TextSpans", Message.translation("server.cozytalefishing.journal.record_none"));
            }
        } else {
            commandBuilder.set("#FishName.TextSpans", Message.translation("server.cozytalefishing.journal.undiscovered_name"));
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
            .build();

        @Nullable
        private String action;
        @Nullable
        private String speciesId;
        @Nullable
        private String waterBodyType;
        @Nullable
        private Boolean checked;
    }
}
