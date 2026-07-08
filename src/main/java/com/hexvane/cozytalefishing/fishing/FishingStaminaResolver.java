package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Resolves fishing fight stamina from the held rod tier. */
public final class FishingStaminaResolver {
  private FishingStaminaResolver() {}

  @Nonnull
  public static FishingRodRegistry.FishingRodStats resolveRodStats(
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Ref<EntityStore> playerRef,
      @Nonnull FishingModConfig config
  ) {
    return FishingRodRegistry.getStatsFromHeld(commandBuffer, playerRef, config);
  }

  public static float resolveMaxStamina(
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Ref<EntityStore> playerRef,
      @Nonnull FishingModConfig config
  ) {
    return resolveRodStats(commandBuffer, playerRef, config).maxStamina();
  }

  public static float resolveRegenPerSecond(
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Ref<EntityStore> playerRef,
      @Nonnull FishingModConfig config
  ) {
    return resolveRodStats(commandBuffer, playerRef, config).regenPerSecond();
  }
}
