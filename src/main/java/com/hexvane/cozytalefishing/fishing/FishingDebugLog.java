package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import java.util.Locale;
import javax.annotation.Nonnull;

/** Server-side fishing debug logging. */
public final class FishingDebugLog {
    private FishingDebugLog() {}

    public static void info(@Nonnull String message, Object... args) {
        log(true, message, args);
    }

    public static void warn(@Nonnull String message, Object... args) {
        log(false, message, args);
    }

    private static void log(boolean info, @Nonnull String message, Object... args) {
        CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
        if (plugin == null) {
            return;
        }
        String formatted = args.length == 0 ? message : String.format(Locale.US, message, args);
        if (info) {
            plugin.getLogger().atInfo().log("[CozyFishing] %s", formatted);
        } else {
            plugin.getLogger().atWarning().log("[CozyFishing] %s", formatted);
        }
    }
}
