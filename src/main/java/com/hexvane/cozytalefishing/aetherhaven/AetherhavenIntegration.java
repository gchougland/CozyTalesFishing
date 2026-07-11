package com.hexvane.cozytalefishing.aetherhaven;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.jewelry.JewelryChestLoot;
import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

/**
 * Soft Aetherhaven hooks. Safe when Aetherhaven is absent (classes may still be on the compile classpath only).
 */
public final class AetherhavenIntegration {
    /** Chance to also roll unidentified jewelry when opening sunken treasure with Aetherhaven present. */
    private static final double SUNKEN_JEWELRY_CHANCE = 0.08;

    private static boolean enabled;
    private static boolean loggedMissing;

    private AetherhavenIntegration() {}

    /** Resolves Aetherhaven on plugin setup; no-ops when the plugin is not loaded. */
    public static void setup() {
        enabled = false;
        try {
            AetherhavenPlugin ah = AetherhavenPlugin.get();
            if (ah == null) {
                logMissing("Aetherhaven not present; skipping soft integration.");
                return;
            }
            enabled = true;
            CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
            if (plugin != null) {
                plugin.getLogger().atInfo().log("Aetherhaven soft integration enabled.");
            }
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            logMissing("Aetherhaven classes unavailable; skipping soft integration.");
        }
    }

    private static void logMissing(@Nonnull String message) {
        if (loggedMissing) {
            return;
        }
        loggedMissing = true;
        CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
        if (plugin != null) {
            plugin.getLogger().atInfo().log(message);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Extra sunken treasure loot when Aetherhaven is loaded: gold coins often, jewelry rarely.
     * Returns an empty list when Aetherhaven is absent.
     */
    @Nonnull
    public static List<ItemStack> rollSunkenTreasureBonus() {
        if (!enabled) {
            return List.of();
        }
        try {
            AetherhavenPlugin ah = AetherhavenPlugin.get();
            if (ah == null) {
                return List.of();
            }
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            List<ItemStack> bonus = new ArrayList<>(2);
            int coins = 1 + rnd.nextInt(4);
            bonus.add(new ItemStack(AetherhavenConstants.ITEM_GOLD_COIN, coins));
            if (rnd.nextDouble() < SUNKEN_JEWELRY_CHANCE) {
                ItemStack jewelry = JewelryChestLoot.rollForChest(rnd, ah.getConfig().get());
                if (jewelry != null && !jewelry.isEmpty()) {
                    bonus.add(jewelry);
                }
            }
            return bonus;
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            return List.of();
        }
    }
}
