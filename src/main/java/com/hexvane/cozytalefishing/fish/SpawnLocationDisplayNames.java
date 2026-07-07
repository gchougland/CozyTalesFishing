package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves spawn environment and biome ids to player-facing labels for the journal UI. */
public final class SpawnLocationDisplayNames {
    private static final String ENV_PREFIX = "server.cozytalefishing.spawn.environment.";
    private static final String BIOME_PREFIX = "server.cozytalefishing.spawn.biome.";

    private SpawnLocationDisplayNames() {}

    @Nonnull
    public static String formatZone(@Nullable String zone) {
        if (zone == null || zone.isBlank()) {
            return "—";
        }
        String trimmed = zone.trim();
        if (trimmed.regionMatches(true, 0, "Zone", 0, 4) && trimmed.length() > 4) {
            return "Zone " + trimmed.substring(4);
        }
        return titleCaseTokens(trimmed.replace('_', ' '));
    }

    @Nonnull
    public static String formatEnvironmentId(@Nullable String environmentId) {
        return resolve(environmentId, ENV_PREFIX, true);
    }

    @Nonnull
    public static String formatBiomeId(@Nullable String biomeId) {
        return resolve(biomeId, BIOME_PREFIX, false);
    }

    @Nonnull
    public static String formatSpawnId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return "—";
        }
        String trimmed = id.trim();
        if (trimmed.startsWith("Env_")) {
            return formatEnvironmentId(trimmed);
        }
        return formatBiomeId(trimmed);
    }

    @Nonnull
    public static String joinEnvironmentIds(@Nullable String[] environmentIds) {
        return joinIds(environmentIds, true);
    }

    @Nonnull
    public static String joinBiomeIds(@Nullable String[] biomeIds) {
        return joinIds(biomeIds, false);
    }

    @Nonnull
    private static String joinIds(@Nullable String[] ids, boolean environments) {
        if (ids == null || ids.length == 0) {
            return "—";
        }
        return Arrays.stream(ids)
            .map(id -> environments ? formatEnvironmentId(id) : formatBiomeId(id))
            .collect(Collectors.joining(", "));
    }

    @Nonnull
    private static String resolve(@Nullable String id, @Nonnull String i18nPrefix, boolean environmentStyle) {
        if (id == null || id.isBlank()) {
            return "—";
        }
        String trimmed = id.trim();
        String localized = lookup(i18nPrefix + trimmed);
        if (localized != null) {
            return localized;
        }
        return environmentStyle ? formatEnvironmentFallback(trimmed) : formatBiomeFallback(trimmed);
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
    private static String formatEnvironmentFallback(@Nonnull String environmentId) {
        String body = environmentId.startsWith("Env_") ? environmentId.substring(4) : environmentId;
        return titleCaseTokens(body.replace('_', ' ').replaceAll("(?i)\\bzone(\\d+)\\b", "Zone $1"));
    }

    @Nonnull
    private static String formatBiomeFallback(@Nonnull String biomeId) {
        if (biomeId.startsWith("Env_")) {
            return formatEnvironmentFallback(biomeId);
        }
        return titleCaseTokens(biomeId.replace('_', ' ').replaceAll("(?i)\\bzone(\\d+)\\b", "Zone $1"));
    }

    @Nonnull
    private static String titleCaseTokens(@Nonnull String text) {
        String[] tokens = text.trim().split("\\s+");
        if (tokens.length == 0) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(formatToken(token));
        }
        return !builder.isEmpty() ? builder.toString() : text;
    }

    @Nonnull
    private static String formatToken(@Nonnull String token) {
        if (token.regionMatches(true, 0, "Zone", 0, 4) && token.length() > 4) {
            String suffix = token.substring(4);
            if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
                return "Zone " + suffix;
            }
        }
        String base = token.replaceAll("\\d+$", "");
        if (base.isEmpty()) {
            base = token;
        }
        if (base.length() == 1) {
            return base.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1).toLowerCase(Locale.ROOT);
    }
}
