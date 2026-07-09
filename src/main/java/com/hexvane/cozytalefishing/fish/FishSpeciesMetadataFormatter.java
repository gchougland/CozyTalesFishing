package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Human-readable spawn metadata for journal UI and debug output. */
public final class FishSpeciesMetadataFormatter {
    private static final String I18N_PREFIX = "server.cozytalefishing.";
    private static final String WATER_BODY_PREFIX = I18N_PREFIX + "spawn.waterbody.";
    private static final String HABITAT_LINE_PREFIX = I18N_PREFIX + "journal.habitat.line.";

    private FishSpeciesMetadataFormatter() {}

    @Nonnull
    public static String formatWaterBodyTypes(@Nonnull WaterBodyType[] types) {
        if (types.length == 0) {
            return "—";
        }
        return Arrays.stream(types).map(FishSpeciesMetadataFormatter::formatWaterBodyType).collect(Collectors.joining(", "));
    }

    @Nonnull
    public static String formatWaterBodyType(@Nonnull WaterBodyType type) {
        String localized = lookup(WATER_BODY_PREFIX + type.name());
        return localized != null ? localized : type.name();
    }

    /** Journal habitat panel: water type, biome/habitat, and depth on separate labeled lines. */
    @Nonnull
    public static String formatHabitat(@Nonnull FishSpeciesAsset species) {
        StringBuilder text = new StringBuilder();
        text.append(formatHabitatLine("water", formatWaterBodyTypes(species.getWaterBodyTypes())));

        String biome = formatSpawnLocation(species);
        if (!biome.equals("—")) {
            text.append('\n').append(formatHabitatLine("biome", biome));
        }

        text.append('\n').append(formatHabitatLine("depth", formatUnderground(species.isUndergroundOnly())));
        return text.toString();
    }

    @Nonnull
    private static String formatHabitatLine(@Nonnull String key, @Nonnull String value) {
        String template = lookup(HABITAT_LINE_PREFIX + key);
        if (template == null) {
            return key + ": " + value;
        }
        return template.replace("{value}", value);
    }

    @Nullable
    private static String lookup(@Nonnull String key) {
        I18nModule i18n = I18nModule.get();
        if (i18n == null) {
            return null;
        }
        String resolved = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, key);
        if (resolved == null || resolved.isBlank() || resolved.equals(key)) {
            return null;
        }
        return resolved;
    }

    @Nonnull
    public static String formatFloatRange(@Nonnull float[] range) {
        if (range.length < 2) {
            return range.length == 1 ? String.format(Locale.US, "%.1f", range[0]) : "—";
        }
        return String.format(Locale.US, "%.0f:00 – %.0f:00", range[0], range[1]);
    }

    @Nonnull
    public static String formatSizeRange(@Nonnull float[] range) {
        if (range.length < 2) {
            return range.length == 1 ? String.format(Locale.US, "%.1f cm", range[0]) : "—";
        }
        float min = Math.min(range[0], range[1]);
        float max = Math.max(range[0], range[1]);
        return String.format(Locale.US, "%.1f – %.1f cm", min, max);
    }

    @Nonnull
    public static String formatSpawnLocation(@Nonnull FishSpeciesAsset species) {
        FishSpawnLocation location = species.getSpawnLocation();
        StringBuilder text = new StringBuilder();
        if (location.hasZone()) {
            text.append(SpawnLocationDisplayNames.formatZone(location.getZone()));
        }
        if (location.hasEnvironments()) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(SpawnLocationDisplayNames.joinEnvironmentIds(location.getEnvironments()));
        }
        if (location.hasBiomes()) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(SpawnLocationDisplayNames.joinBiomeIds(location.getBiomes()));
        }
        return !text.isEmpty() ? text.toString() : "—";
    }

    @Nonnull
    public static String formatWeather(@Nullable String[] weatherIds) {
        if (weatherIds == null || weatherIds.length == 0) {
            return "Any";
        }
        return WeatherDisplayNames.joinWeatherIds(weatherIds);
    }

    @Nonnull
    public static String formatRuleMode(@Nullable FishSpawnRuleMode mode) {
        if (mode == null || mode == FishSpawnRuleMode.Ignored) {
            return "any";
        }
        return mode.name().toLowerCase(Locale.US);
    }

    @Nonnull
    public static String formatDayTimeRule(@Nonnull FishSpawnRules.DayTimeRule rule) {
        FishSpawnRuleMode mode = rule.getMode() != null ? rule.getMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored) {
            return "Any time";
        }
        float[] range = rule.getRange();
        String rangeText = range != null && range.length >= 2 ? formatFloatRange(range) : "—";
        return rangeText + " (" + formatRuleMode(mode) + ")";
    }

    @Nonnull
    public static String formatWeatherRule(@Nonnull FishSpawnRules.WeatherRule rule) {
        FishSpawnRuleMode mode = rule.getMode() != null ? rule.getMode() : FishSpawnRuleMode.Ignored;
        if (mode == FishSpawnRuleMode.Ignored || !rule.hasIds()) {
            return "Any weather";
        }
        return formatWeather(rule.getIds()) + " (" + formatRuleMode(mode) + ")";
    }

    @Nonnull
    public static String formatSpawnRules(@Nonnull FishSpeciesAsset species) {
        FishSpawnRules rules = species.getSpawnRules();
        StringBuilder text = new StringBuilder();
        text.append("Time: ").append(formatDayTimeRule(rules.getDayTime()));
        text.append("\nWeather: ").append(formatWeatherRule(rules.getWeather()));
        FishSpawnRules.UndergroundRule underground = rules.getUnderground();
        if (underground.getMode() != null && underground.getMode() != FishSpawnRuleMode.Ignored) {
            text.append("\nDepth: ").append(formatUnderground(underground.getOnly() != null && underground.getOnly()));
            text.append(" (").append(formatRuleMode(underground.getMode())).append(')');
        }
        return text.toString();
    }

    @Nonnull
    public static String formatUnderground(boolean undergroundOnly) {
        return undergroundOnly ? "Underground / caves" : "Surface";
    }

    @Nonnull
    public static String formatRarity(@Nonnull FishRarity rarity) {
        return rarity.name();
    }

    @Nonnull
    public static String formatShadowType(@Nonnull FishShadowType shadowType) {
        return shadowType.name();
    }

    @Nonnull
    public static String formatShadowType(@Nonnull FishSpeciesAsset species) {
        if (species.usesRandomShadowType()) {
            return "Random";
        }
        return species.getShadowType().name();
    }

    @Nonnull
    public static String formatPersonalBest(float largestSizeCm) {
        if (largestSizeCm <= 0.0f) {
            return "—";
        }
        return String.format(Locale.US, "%.1f cm", largestSizeCm);
    }
}
