package com.hexvane.cozytalefishing.integration;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hexvane.cozytalefishing.fish.FishRarity;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.common.semver.SemverRange;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Awards FISHING XP through {@code MMOSkillTreeAPI} when MMO Skill Tree is installed.
 *
 * <p>MMO runs in a separate plugin classloader — never use {@code Class.forName} on the caller class; load via
 * {@link CozyTalesFishingPlugin#getClassLoader()} so the bridge can see optional dependency {@code Ziggfreed:MMOSkillTree}.
 *
 * <p>The {@link com.hexvane.cozytalefishing.integration.mmo.CozyTalesFishingMmoAdapter} class is the
 * {@code FishingIntegration} adapter to register inside MMO Skill Tree (see integration docs).
 */
final class MMOSkillTreeIntegration {
    private static final PluginIdentifier MMO_PLUGIN_ID = PluginIdentifier.fromString("Ziggfreed:MMOSkillTree");
    private static final String SKILL_FISHING = "FISHING";
    private static final String API_CLASS = "com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI";
    private static final double MIN_XP = 5.0;
    private static final double NEW_SPECIES_BONUS = 25.0;

    private static volatile boolean enabled;
    private static volatile boolean loggedWaiting;
    private static volatile boolean loggedEnabled;

    private static Method addXpWithNotification;
    private static Method addXpPlain;
    private static Method getOrCreateSkillComponent;

    private MMOSkillTreeIntegration() {}

    static void setup() {
        if (enabled) {
            return;
        }
        addXpWithNotification = null;
        addXpPlain = null;
        getOrCreateSkillComponent = null;

        CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
        if (plugin == null) {
            return;
        }

        if (!isMmoPluginLoaded()) {
            logWaiting("MMO Skill Tree plugin not loaded yet; fishing XP integration will retry on catch.");
            return;
        }

        Class<?> api = loadMmoClass(plugin, API_CLASS);
        if (api == null) {
            logWaiting(
                "MMO Skill Tree is loaded but "
                    + API_CLASS
                    + " is not visible — check OptionalDependencies uses Ziggfreed:MMOSkillTree."
            );
            return;
        }

        resolveAddXpMethods(api);
        getOrCreateSkillComponent = findGetOrCreateSkillComponent(api);
        if (addXpPlain == null && addXpWithNotification == null) {
            logWaiting("MMO Skill Tree API found but addXp could not be resolved.");
            return;
        }

        enabled = true;
        logEnabled("MMO Skill Tree integration enabled (FISHING XP on journal catches).");
    }

    static void handleFishCaught(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef playerRefComponent,
        @Nonnull CozyTalesFishingIntegration.FishCaughtEvent event
    ) {
        if (!enabled) {
            setup();
        }
        if (!enabled) {
            return;
        }
        Store<EntityStore> store = commandBuffer.getStore();
        if (store == null) {
            return;
        }

        long xp = Math.max(1, Math.round(computeXp(event)));

        ensureSkillComponent(store, commandBuffer, playerRef);
        if (!awardFishingXp(store, playerRef, xp)) {
            ensureSkillComponent(store, commandBuffer, playerRef);
            awardFishingXp(store, playerRef, xp);
        }
    }

    private static boolean isMmoPluginLoaded() {
        try {
            return PluginManager.get().hasPlugin(MMO_PLUGIN_ID, SemverRange.WILDCARD);
        } catch (RuntimeException e) {
            return PluginManager.get().getPlugin(MMO_PLUGIN_ID) != null;
        }
    }

    @Nullable
    private static Class<?> loadMmoClass(@Nonnull CozyTalesFishingPlugin plugin, @Nonnull String className) {
        try {
            return plugin.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static double computeXp(@Nonnull CozyTalesFishingIntegration.FishCaughtEvent event) {
        FishRarity rarity = resolveRarity(event);
        double xp = CozyTalesFishingIntegration.defaultXpForRarity(rarity);
        if (event.newSpecies()) {
            xp += NEW_SPECIES_BONUS;
        }
        return Math.max(MIN_XP, xp);
    }

    @Nonnull
    private static FishRarity resolveRarity(@Nonnull CozyTalesFishingIntegration.FishCaughtEvent event) {
        var species = FishSpeciesRegistry.getSpecies(event.speciesId());
        if (species != null) {
            return species.getRarity();
        }
        int ordinal = event.rarityOrdinal();
        if (ordinal >= 0 && ordinal < FishRarity.values().length) {
            return FishRarity.values()[ordinal];
        }
        return FishRarity.Common;
    }

    private static void resolveAddXpMethods(@Nonnull Class<?> api) {
        for (Method method : api.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("addXp")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            boolean numericAmount =
                params[3] == long.class || params[3] == double.class || Number.class.isAssignableFrom(params[3]);
            if (params.length == 4
                && Store.class.isAssignableFrom(params[0])
                && Ref.class.isAssignableFrom(params[1])
                && params[2] == String.class
                && numericAmount) {
                method.setAccessible(true);
                addXpPlain = method;
            }
            if (params.length == 5
                && Store.class.isAssignableFrom(params[0])
                && Ref.class.isAssignableFrom(params[1])
                && params[2] == String.class
                && numericAmount
                && params[4] == boolean.class) {
                method.setAccessible(true);
                addXpWithNotification = method;
            }
        }
    }

    @Nullable
    private static Method findGetOrCreateSkillComponent(@Nonnull Class<?> api) {
        for (Method method : api.getMethods()) {
            if (!method.getName().equals("getOrCreateSkillComponent")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 3
                && Store.class.isAssignableFrom(params[0])
                && CommandBuffer.class.isAssignableFrom(params[1])
                && Ref.class.isAssignableFrom(params[2])) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static void ensureSkillComponent(
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        if (getOrCreateSkillComponent == null) {
            return;
        }
        try {
            getOrCreateSkillComponent.invoke(null, store, commandBuffer, playerRef);
        } catch (ReflectiveOperationException ignored) {
            // optional
        }
    }

    private static boolean awardFishingXp(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef, long xp) {
        try {
            if (addXpWithNotification != null) {
                Object result = addXpWithNotification.invoke(null, store, playerRef, SKILL_FISHING, xp, true);
                if (result instanceof Boolean b) {
                    return b;
                }
                return true;
            }
            if (addXpPlain != null) {
                Object result = addXpPlain.invoke(null, store, playerRef, SKILL_FISHING, xp);
                return !(result instanceof Boolean b) || b;
            }
        } catch (ReflectiveOperationException ignored) {
            // soft integration
        }
        return false;
    }

    private static void logWaiting(@Nonnull String message) {
        if (loggedWaiting || enabled) {
            return;
        }
        loggedWaiting = true;
        log(message);
    }

    private static void logEnabled(@Nonnull String message) {
        if (loggedEnabled) {
            return;
        }
        loggedEnabled = true;
        log(message);
    }

    private static void log(@Nonnull String message) {
        CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
        if (plugin != null) {
            plugin.getLogger().atInfo().log(message);
        }
    }
}
