package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Evaluates {@link FishSpawnRules} against a spawn column and world state. */
public final class FishSpawnRulesEvaluator {
    private FishSpawnRulesEvaluator() {}

    public record SpawnEvaluationContext(
        @Nonnull WaterBodyType bodyType,
        int environmentIndex,
        int blockX,
        int blockZ,
        int surfaceY,
        @Nonnull World world,
        @Nonnull FishShadowSpawnHelper.SpawnConditions conditions,
        @Nullable FishingSpawnRegionContext regionContext,
        @Nonnull FishingModConfig config
    ) {}

    public record SpawnRuleResult(
        boolean eligible,
        float weightMultiplier,
        @Nonnull Map<String, Boolean> ruleMatches
    ) {
        @Nonnull
        public static SpawnRuleResult ineligible() {
            return new SpawnRuleResult(false, 0.0f, Map.of());
        }
    }

    @Nonnull
    public static SpawnRuleResult evaluate(@Nonnull FishSpeciesAsset species, @Nonnull SpawnEvaluationContext context) {
        FishSpawnRules rules = species.getSpawnRules();
        Map<String, Boolean> matches = new LinkedHashMap<>();
        float weightMultiplier = 1.0f;

        RuleOutcome waterBody = evaluateWaterBody(rules, resolveSpawnWaterBody(context));
        if (!applyOutcome("WaterBody", waterBody, matches)) {
            return SpawnRuleResult.ineligible();
        }
        weightMultiplier *= waterBody.weightMultiplier(context.config());

        RuleOutcome zone = evaluateZone(rules, species, context);
        if (!applyOutcome("Zone", zone, matches)) {
            return SpawnRuleResult.ineligible();
        }
        weightMultiplier *= zone.weightMultiplier(context.config());

        RuleOutcome environment = evaluateEnvironment(rules, species, context);
        if (!applyOutcome("Environment", environment, matches)) {
            return SpawnRuleResult.ineligible();
        }
        weightMultiplier *= environment.weightMultiplier(context.config());

        RuleOutcome underground = evaluateUnderground(rules, species, context);
        if (!applyOutcome("Underground", underground, matches)) {
            return SpawnRuleResult.ineligible();
        }
        weightMultiplier *= underground.weightMultiplier(context.config());

        RuleOutcome dayTime = evaluateDayTime(rules, context.conditions().worldTime());
        if (!applyOutcome("DayTime", dayTime, matches)) {
            return SpawnRuleResult.ineligible();
        }
        weightMultiplier *= dayTime.weightMultiplier(context.config());

        RuleOutcome weather = evaluateWeather(rules, context.conditions().weatherIndex());
        if (!applyOutcome("Weather", weather, matches)) {
            return SpawnRuleResult.ineligible();
        }
        weightMultiplier *= weather.weightMultiplier(context.config());

        return new SpawnRuleResult(true, weightMultiplier, matches);
    }

    /** Like {@link #evaluate} but evaluates every rule without short-circuiting (for spawn probes). */
    @Nonnull
    public static SpawnRuleResult evaluateDetailed(
        @Nonnull FishSpeciesAsset species,
        @Nonnull SpawnEvaluationContext context
    ) {
        FishSpawnRules rules = species.getSpawnRules();
        Map<String, Boolean> matches = new LinkedHashMap<>();
        float weightMultiplier = 1.0f;
        boolean eligible = true;

        RuleOutcome waterBody = evaluateWaterBody(rules, resolveSpawnWaterBody(context));
        eligible &= recordOutcome("WaterBody", waterBody, matches);
        weightMultiplier *= waterBody.weightMultiplier(context.config());

        RuleOutcome zone = evaluateZone(rules, species, context);
        eligible &= recordOutcome("Zone", zone, matches);
        weightMultiplier *= zone.weightMultiplier(context.config());

        RuleOutcome environment = evaluateEnvironment(rules, species, context);
        eligible &= recordOutcome("Environment", environment, matches);
        weightMultiplier *= environment.weightMultiplier(context.config());

        RuleOutcome underground = evaluateUnderground(rules, species, context);
        eligible &= recordOutcome("Underground", underground, matches);
        weightMultiplier *= underground.weightMultiplier(context.config());

        RuleOutcome dayTime = evaluateDayTime(rules, context.conditions().worldTime());
        eligible &= recordOutcome("DayTime", dayTime, matches);
        weightMultiplier *= dayTime.weightMultiplier(context.config());

        RuleOutcome weather = evaluateWeather(rules, context.conditions().weatherIndex());
        eligible &= recordOutcome("Weather", weather, matches);
        weightMultiplier *= weather.weightMultiplier(context.config());

        return new SpawnRuleResult(eligible, weightMultiplier, matches);
    }

    private static boolean recordOutcome(
        @Nonnull String key,
        @Nonnull RuleOutcome outcome,
        @Nonnull Map<String, Boolean> matches
    ) {
        if (outcome.mode() == FishSpawnRuleMode.Ignored) {
            return true;
        }
        if (outcome.mode() == FishSpawnRuleMode.Required) {
            matches.put(key, outcome.eligible());
            return outcome.eligible();
        }
        matches.put(key, outcome.preferredMatched());
        return true;
    }

    private record RuleOutcome(
        @Nonnull FishSpawnRuleMode mode,
        boolean eligible,
        boolean preferredMatched
    ) {
        float weightMultiplier(@Nonnull FishingModConfig config) {
            if (mode != FishSpawnRuleMode.Preferred) {
                return 1.0f;
            }
            return preferredMatched
                ? config.getSpawnPreferredMatchWeightMultiplier()
                : config.getSpawnPreferredMissWeightMultiplier();
        }
    }

    private static boolean applyOutcome(
        @Nonnull String key,
        @Nonnull RuleOutcome outcome,
        @Nonnull Map<String, Boolean> matches
    ) {
        if (outcome.mode() == FishSpawnRuleMode.Ignored) {
            return true;
        }
        if (outcome.mode() == FishSpawnRuleMode.Required) {
            matches.put(key, outcome.eligible());
            return outcome.eligible();
        }
        matches.put(key, outcome.preferredMatched());
        return outcome.eligible();
    }

    @Nonnull
    private static WaterBodyType resolveSpawnWaterBody(@Nonnull SpawnEvaluationContext context) {
        String biomeName = WaterBodyClassifier.getBiomeName(context.world(), context.blockX(), context.blockZ());
        if (context.regionContext() != null && context.regionContext().getEffectiveBiome() != null) {
            biomeName = context.regionContext().getEffectiveBiome();
        }
        return WaterBodyClassifier.correctForOceanContext(
            context.bodyType(),
            biomeName,
            context.environmentIndex()
        );
    }

    @Nonnull
    private static RuleOutcome evaluateWaterBody(@Nonnull FishSpawnRules rules, @Nonnull WaterBodyType bodyType) {
        FishSpawnRules.WaterBodyRule rule = rules.getWaterBody();
        FishSpawnRuleMode mode = rule.getMode() != null ? rule.getMode() : FishSpawnRuleMode.Required;
        if (mode == FishSpawnRuleMode.Ignored) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }
        boolean matched = rules.matchesWaterBody(bodyType);
        return outcomeForMode(mode, matched);
    }

    @Nonnull
    private static RuleOutcome evaluateZone(
        @Nonnull FishSpawnRules rules,
        @Nonnull FishSpeciesAsset species,
        @Nonnull SpawnEvaluationContext context
    ) {
        FishSpawnRules.LocationRule location = rules.getLocation();
        FishSpawnRuleMode mode = location.getZoneMode() != null ? location.getZoneMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored || !location.hasZone()) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }

        String worldZone = resolveWorldZone(context);
        String speciesZone = FishShadowSpawnHelper.getSpeciesZonePrefix(species);
        if (speciesZone == null) {
            speciesZone = location.getZone();
        }
        boolean matched =
            worldZone != null
                && speciesZone != null
                && FishShadowSpawnHelper.zonesMatch(speciesZone, worldZone)
                && FishShadowSpawnHelper.speciesSupportsWorldZone(species, worldZone);
        return outcomeForMode(mode, matched);
    }

    @Nonnull
    private static RuleOutcome evaluateEnvironment(
        @Nonnull FishSpawnRules rules,
        @Nonnull FishSpeciesAsset species,
        @Nonnull SpawnEvaluationContext context
    ) {
        FishSpawnRules.LocationRule location = rules.getLocation();
        FishSpawnRuleMode mode =
            location.getEnvironmentMode() != null ? location.getEnvironmentMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }
        if (!location.hasEnvironments() && !location.hasBiomes() && !rules.getLocation().hasZone()) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }

        boolean strictMatch =
            FishShadowSpawnHelper.matchesSpawnEnvironment(
                species,
                context.environmentIndex(),
                context.world(),
                context.blockX(),
                context.blockZ(),
                FishShadowSpawnHelper.EnvironmentMatchMode.STRICT,
                context.regionContext()
            );
        if (strictMatch) {
            return outcomeForMode(mode, true);
        }

        if (mode == FishSpawnRuleMode.Required) {
            return new RuleOutcome(mode, false, false);
        }

        boolean zoneMatch =
            FishShadowSpawnHelper.matchesSpawnEnvironment(
                species,
                context.environmentIndex(),
                context.world(),
                context.blockX(),
                context.blockZ(),
                FishShadowSpawnHelper.EnvironmentMatchMode.ZONE_ONLY,
                context.regionContext()
            );

        if (zoneMatch) {
            return new RuleOutcome(mode, true, false);
        }
        return new RuleOutcome(mode, false, false);
    }

    @Nonnull
    private static RuleOutcome evaluateUnderground(
        @Nonnull FishSpawnRules rules,
        @Nonnull FishSpeciesAsset species,
        @Nonnull SpawnEvaluationContext context
    ) {
        FishSpawnRules.UndergroundRule rule = rules.getUnderground();
        FishSpawnRuleMode mode = rule.getMode() != null ? rule.getMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }

        boolean underground =
            FishShadowSpawnHelper.isUnderground(
                context.world(),
                context.blockX(),
                context.blockZ(),
                context.surfaceY(),
                context.config().getUndergroundSurfaceOffset()
            );
        boolean wantsUnderground = rule.getOnly() != null && rule.getOnly();
        boolean matched = wantsUnderground == underground;

        String zonePrefix =
            context.regionContext() != null && context.regionContext().getEffectiveZonePrefix() != null
                ? context.regionContext().getEffectiveZonePrefix()
                : FishShadowSpawnHelper.getWorldZonePrefix(context.world(), context.blockX(), context.blockZ());
        if (zonePrefix != null && zonePrefix.equalsIgnoreCase("Zone4") && !underground) {
            matched = false;
        }
        if (FishSpeciesRegistry.requiresUndergroundSpawn(species) && !underground) {
            matched = false;
        }

        return outcomeForMode(mode, matched);
    }

    @Nonnull
    private static RuleOutcome evaluateDayTime(
        @Nonnull FishSpawnRules rules,
        @Nonnull WorldTimeResource worldTime
    ) {
        FishSpawnRules.DayTimeRule rule = rules.getDayTime();
        FishSpawnRuleMode mode = rule.getMode() != null ? rule.getMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }
        float[] range = rule.getRange();
        if (range == null || range.length < 2) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }
        boolean matched = FishShadowSpawnHelper.matchesDayTimeRange(range, worldTime);
        return outcomeForMode(mode, matched);
    }

    @Nonnull
    private static RuleOutcome evaluateWeather(@Nonnull FishSpawnRules rules, int currentWeatherIndex) {
        FishSpawnRules.WeatherRule rule = rules.getWeather();
        FishSpawnRuleMode mode = rule.getMode() != null ? rule.getMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored || !rule.hasIds()) {
            return new RuleOutcome(FishSpawnRuleMode.Ignored, true, true);
        }
        boolean matched = FishShadowSpawnHelper.matchesWeather(rule.getWeatherIndexes(), currentWeatherIndex);
        return outcomeForMode(mode, matched);
    }

    @Nonnull
    private static RuleOutcome outcomeForMode(@Nonnull FishSpawnRuleMode mode, boolean matched) {
        if (mode == FishSpawnRuleMode.Ignored) {
            return new RuleOutcome(mode, true, true);
        }
        if (mode == FishSpawnRuleMode.Required) {
            return new RuleOutcome(mode, matched, matched);
        }
        return new RuleOutcome(mode, true, matched);
    }

    @Nullable
    private static String resolveWorldZone(@Nonnull SpawnEvaluationContext context) {
        if (context.regionContext() != null && context.regionContext().getEffectiveZonePrefix() != null) {
            return context.regionContext().getEffectiveZonePrefix();
        }
        return FishShadowSpawnHelper.getWorldZonePrefix(context.world(), context.blockX(), context.blockZ());
    }
}
