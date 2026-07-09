package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves which fish species can spawn at a probe location (for /cozyfish region probe). */
public final class SpawnProbeService {
    private SpawnProbeService() {}

    public record SpeciesProbeEntry(
        @Nonnull FishSpeciesAsset species,
        float weightMultiplier,
        float effectiveWeight,
        @Nonnull Map<String, Boolean> ruleMatches
    ) {}

    public record ProbeResult(
        @Nonnull WaterBodyType spawnWaterBody,
        boolean underground,
        @Nonnull FishShadowSpawnHelper.SpawnConditions spawnConditions,
        int waterBodyPoolSize,
        @Nonnull List<SpeciesProbeEntry> eligibleNow,
        @Nonnull List<SpeciesProbeEntry> timeBlockedOnly
    ) {}

    @Nonnull
    public static ProbeResult analyze(
        @Nonnull World world,
        int blockX,
        int blockZ,
        int surfaceY,
        int environmentIndex,
        @Nonnull WaterBodyType classifiedWaterBody,
        @Nullable FishingSpawnRegionContext regionContext
    ) {
        FishingModConfig config = FishingModConfig.get();
        String biomeName = WaterBodyClassifier.getBiomeName(world, blockX, blockZ);
        if (regionContext != null && regionContext.getEffectiveBiome() != null) {
            biomeName = regionContext.getEffectiveBiome();
        }

        WaterBodyType spawnWaterBody =
            WaterBodyClassifier.correctForOceanContext(classifiedWaterBody, biomeName, environmentIndex);
        if (regionContext != null && regionContext.getWaterBodyOverride() != null) {
            spawnWaterBody = regionContext.getWaterBodyOverride();
        }

        boolean underground =
            FishShadowSpawnHelper.isUnderground(
                world,
                blockX,
                blockZ,
                surfaceY,
                config.getUndergroundSurfaceOffset()
            );

        FishShadowSpawnHelper.SpawnConditions spawnConditions =
            FishShadowSpawnHelper.resolveSpawnConditions(world, environmentIndex);
        FishSpawnRulesEvaluator.SpawnEvaluationContext context =
            new FishSpawnRulesEvaluator.SpawnEvaluationContext(
                spawnWaterBody,
                environmentIndex,
                blockX,
                blockZ,
                surfaceY,
                world,
                spawnConditions,
                regionContext,
                config
            );

        List<FishSpeciesAsset> pool = FishSpeciesRegistry.getSpeciesForWaterBody(spawnWaterBody);
        List<SpeciesProbeEntry> eligibleNow = new ArrayList<>();
        List<SpeciesProbeEntry> timeBlockedOnly = new ArrayList<>();

        for (FishSpeciesAsset species : pool) {
            if (species.isTrash() || species.isTreasure()) {
                continue;
            }

            FishSpawnRulesEvaluator.SpawnRuleResult result =
                FishSpawnRulesEvaluator.evaluateDetailed(species, context);
            float effectiveWeight =
                species.getEffectiveSpawnWeight(config.getGlobalSpawnWeightMultiplier()) * result.weightMultiplier();

            SpeciesProbeEntry entry =
                new SpeciesProbeEntry(species, result.weightMultiplier(), effectiveWeight, result.ruleMatches());

            if (result.eligible()) {
                eligibleNow.add(entry);
            } else if (isTimeBlockedOnly(result)) {
                timeBlockedOnly.add(entry);
            }
        }

        eligibleNow.sort(byEffectiveWeightDescending());
        timeBlockedOnly.sort(byEffectiveWeightDescending());

        return new ProbeResult(
            spawnWaterBody,
            underground,
            spawnConditions,
            pool.size(),
            eligibleNow,
            timeBlockedOnly
        );
    }

    public static void appendSpeciesSections(@Nonnull StringBuilder text, @Nonnull ProbeResult result) {
        WorldTimeResource worldTime = result.spawnConditions().worldTime();
        text.append("  currentHour=").append(worldTime.getCurrentHour());
        text.append(", weatherIndex=").append(result.spawnConditions().weatherIndex());
        text.append(", underground=").append(result.underground()).append('\n');
        text.append("  spawnWaterBody=").append(result.spawnWaterBody().name());
        text.append(", speciesPool=").append(result.waterBodyPoolSize());
        text.append(", eligibleNow=").append(result.eligibleNow().size());
        text.append(", timeBlockedOnly=").append(result.timeBlockedOnly().size()).append('\n');

        appendSpeciesList(text, "Eligible now", result.eligibleNow(), true);
        appendSpeciesList(text, "Eligible at other times (DayTime only)", result.timeBlockedOnly(), false);

        if (result.eligibleNow().isEmpty() && result.timeBlockedOnly().isEmpty()) {
            if (result.spawnWaterBody() == WaterBodyType.Lava) {
                text.append("  >> No fish match this lava location.\n");
            } else {
                text.append("  >> No fish match this location — spawns fall back to trash.\n");
            }
        } else if (result.eligibleNow().isEmpty()) {
            if (result.spawnWaterBody() == WaterBodyType.Lava) {
                text.append("  >> No fish eligible right now.\n");
            } else {
                text.append("  >> No fish eligible right now — spawns fall back to trash until conditions change.\n");
            }
        }
    }

    private static void appendSpeciesList(
        @Nonnull StringBuilder text,
        @Nonnull String heading,
        @Nonnull List<SpeciesProbeEntry> entries,
        boolean showWeightMultiplier
    ) {
        text.append('\n').append(heading).append(" (").append(entries.size()).append("):\n");
        if (entries.isEmpty()) {
            text.append("  (none)\n");
            return;
        }

        for (SpeciesProbeEntry entry : entries) {
            FishSpeciesAsset species = entry.species();
            text.append("  - ").append(species.getId());
            text.append(" [").append(species.getRarity().name()).append(']');
            text.append(", weight=").append(species.getSpawnWeight());
            if (showWeightMultiplier && entry.weightMultiplier() != 1.0f) {
                text.append(", spawnMult=")
                    .append(String.format(Locale.US, "%.2f", entry.weightMultiplier()));
            }
            text.append(", effective=")
                .append(String.format(Locale.US, "%.1f", entry.effectiveWeight()));

            FishSpawnRules.DayTimeRule dayTime = species.getSpawnRules().getDayTime();
            if (dayTime.getMode() == FishSpawnRuleMode.Required && dayTime.getRange() != null) {
                text.append(", hours=")
                    .append(FishSpeciesMetadataFormatter.formatDayTimeRule(dayTime));
            }
            text.append('\n');
        }
    }

    private static boolean isTimeBlockedOnly(@Nonnull FishSpawnRulesEvaluator.SpawnRuleResult result) {
        if (result.eligible()) {
            return false;
        }
        Map<String, Boolean> matches = result.ruleMatches();
        if (!Boolean.FALSE.equals(matches.get("DayTime"))) {
            return false;
        }
        for (Map.Entry<String, Boolean> entry : matches.entrySet()) {
            if ("DayTime".equals(entry.getKey())) {
                continue;
            }
            if (Boolean.FALSE.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    private static Comparator<SpeciesProbeEntry> byEffectiveWeightDescending() {
        return Comparator.comparing(SpeciesProbeEntry::effectiveWeight).reversed();
    }
}
