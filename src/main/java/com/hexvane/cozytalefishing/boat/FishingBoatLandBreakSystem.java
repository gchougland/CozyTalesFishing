package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Breaks fishing boat entities into items when they leave water. */
public final class FishingBoatLandBreakSystem extends EntityTickingSystem<EntityStore> {
  @Nonnull
  private final Query<EntityStore> query =
      Query.and(FishingBoatComponent.getComponentType(), TransformComponent.getComponentType());

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
    FishingBoatComponent boat = archetypeChunk.getComponent(index, FishingBoatComponent.getComponentType());
    TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
    if (boat == null || transform == null || boat.isSuppressBlockPlacement()) {
      return;
    }

    World world = store.getExternalData().getWorld();
    var pos = transform.getPosition();
    if (BoatWaterHelper.isFloatingOnWater(world, pos.x, pos.y, pos.z)) {
      return;
    }

    FishingBoatDropHelper.dropBoatAsItem(
        archetypeChunk.getReferenceTo(index),
        boat,
        transform,
        commandBuffer,
        true
    );
  }
}
