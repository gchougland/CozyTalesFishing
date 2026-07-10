package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Prevents mounted fishing boat entities from coasting on leftover physics velocity. */
public final class FishingBoatDriftStopSystem extends EntityTickingSystem<EntityStore> {
  @Nonnull
  private final Query<EntityStore> query =
      Query.and(
          FishingBoatComponent.getComponentType(),
          NPCMountComponent.getComponentType(),
          Velocity.getComponentType()
      );

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return query;
  }

  @Override
  public void tick(
      float dt,
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer
  ) {
    Velocity velocity = archetypeChunk.getComponent(index, Velocity.getComponentType());
    if (velocity != null) {
      velocity.setZero();
    }
  }
}
