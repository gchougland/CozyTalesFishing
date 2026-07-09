package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Clears physics velocity on fishing boat entities. */
public final class FishingBoatVelocityHelper {
  private FishingBoatVelocityHelper() {}

  public static void zeroVelocity(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull ComponentAccessor<EntityStore> accessor
  ) {
    Velocity velocity = accessor.getComponent(ref, Velocity.getComponentType());
    if (velocity != null) {
      velocity.setZero();
    }
  }
}
