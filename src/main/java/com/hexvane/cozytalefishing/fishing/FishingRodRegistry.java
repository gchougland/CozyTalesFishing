package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-tier fishing rod fight stamina stats.
 * Tooltip values in server.lang must match these numbers.
 */
public final class FishingRodRegistry {
  public static final String WOODEN_ROD_ID = "CozyFishing_Wooden_Rod";
  public static final String IRON_ROD_ID = "CozyFishing_Iron_Rod";
  public static final String THORIUM_ROD_ID = "CozyFishing_Thorium_Rod";
  public static final String COBALT_ROD_ID = "CozyFishing_Cobalt_Rod";
  public static final String ADAMANTITE_ROD_ID = "CozyFishing_Adamantite_Rod";

  private static final Map<String, FishingRodStats> BY_ITEM_ID =
      Map.of(
          WOODEN_ROD_ID, new FishingRodStats(WOODEN_ROD_ID, 110.0f, 22.0f),
          IRON_ROD_ID, new FishingRodStats(IRON_ROD_ID, 125.0f, 24.0f),
          THORIUM_ROD_ID, new FishingRodStats(THORIUM_ROD_ID, 168.0f, 32.0f),
          COBALT_ROD_ID, new FishingRodStats(COBALT_ROD_ID, 180.0f, 26.0f),
          ADAMANTITE_ROD_ID, new FishingRodStats(ADAMANTITE_ROD_ID, 220.0f, 38.0f));

  private FishingRodRegistry() {}

  public static boolean isFishingRod(@Nullable String itemId) {
    return itemId != null && BY_ITEM_ID.containsKey(itemId);
  }

  public static float getTrashSpawnChance(@Nullable String rodId, @Nonnull FishingModConfig config) {
    if (WOODEN_ROD_ID.equals(rodId)) {
      return config.getWoodenTrashSpawnChance();
    }
    if (IRON_ROD_ID.equals(rodId)) {
      return config.getIronTrashSpawnChance();
    }
    return 0.0f;
  }

  @Nonnull
  public static FishingRodStats getStats(@Nonnull String itemId, @Nonnull FishingModConfig config) {
    FishingRodStats stats = BY_ITEM_ID.get(itemId);
    if (stats != null) {
      return stats;
    }
    return new FishingRodStats(itemId, config.getMaxFishingStamina(), config.getFishingStaminaRegenPerSecond());
  }

  @Nonnull
  public static FishingRodStats getStatsFromHeld(
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Ref<EntityStore> playerRef,
      @Nonnull FishingModConfig config
  ) {
    ItemStack held = InventoryComponent.getItemInHand(commandBuffer, playerRef);
    if (held == null || held.isEmpty()) {
      return new FishingRodStats("", config.getMaxFishingStamina(), config.getFishingStaminaRegenPerSecond());
    }
    return getStats(held.getItemId(), config);
  }

  public record FishingRodStats(@Nonnull String itemId, float maxStamina, float regenPerSecond) {}
}
