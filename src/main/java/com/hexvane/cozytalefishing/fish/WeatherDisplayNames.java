package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves weather asset ids to player-facing labels for the journal UI. */
public final class WeatherDisplayNames {
    private static final String WEATHER_PREFIX = "server.cozytalefishing.spawn.weather.";

    private WeatherDisplayNames() {}

    @Nonnull
    public static String formatWeatherId(@Nullable String weatherId) {
        return resolve(weatherId);
    }

    @Nonnull
    public static String joinWeatherIds(@Nullable String[] weatherIds) {
        if (weatherIds == null || weatherIds.length == 0) {
            return "—";
        }
        return Arrays.stream(weatherIds).map(WeatherDisplayNames::formatWeatherId).collect(Collectors.joining(", "));
    }

    @Nonnull
    private static String resolve(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return "—";
        }
        String trimmed = id.trim();
        String localized = lookup(WEATHER_PREFIX + trimmed);
        if (localized != null) {
            return localized;
        }
        return formatWeatherFallback(trimmed);
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
    private static String formatWeatherFallback(@Nonnull String weatherId) {
        String body = weatherId.replaceAll("^Zone\\d+_", "");
        return switch (body) {
            case "Rain_Light" -> "Light Rain";
            case "Rain_Heavy" -> "Heavy Rain";
            case "Thunder_Storm" -> "Thunderstorm";
            case "Sand_Storm" -> "Sandstorm";
            case "Snow_Storm" -> "Blizzard";
            case "Northern_Lights" -> "Northern Lights";
            default -> titleCaseTokens(body.replace('_', ' '));
        };
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
            if (token.length() == 1) {
                builder.append(token.toUpperCase(Locale.ROOT));
            } else {
                builder.append(Character.toUpperCase(token.charAt(0)))
                    .append(token.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return !builder.isEmpty() ? builder.toString() : text;
    }
}
