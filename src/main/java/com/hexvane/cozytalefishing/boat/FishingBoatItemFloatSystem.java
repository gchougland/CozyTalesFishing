package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Keeps dropped fishing boat items floating on the water surface. */
public final class FishingBoatItemFloatSystem extends EntityTickingSystem<EntityStore> {
  @Nonnull
  private final Query<EntityStore> query =
      Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());

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
    ItemComponent item = archetypeChunk.getComponent(index, ItemComponent.getComponentType());
    TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
    if (item == null || transform == null || item.getItemStack().isEmpty()) {
      return;
    }
    if (!FishingBoatComponent.DEFAULT_SOURCE_ITEM.equals(item.getItemStack().getItemId())) {
      return;
    }

    World world = store.getExternalData().getWorld();
    Vector3d pos = transform.getPosition();
    double surfaceY = BoatWaterHelper.deckYForPosition(world, pos.x, pos.z);
    if (Double.isNaN(surfaceY)) {
      return;
    }

    if (Math.abs(pos.y - surfaceY) > 0.02) {
      transform.setPosition(new Vector3d(pos.x, surfaceY, pos.z));
    }

    Velocity velocity = commandBuffer.getComponent(archetypeChunk.getReferenceTo(index), Velocity.getComponentType());
    if (velocity != null) {
      velocity.setZero();
    }
  }
}
