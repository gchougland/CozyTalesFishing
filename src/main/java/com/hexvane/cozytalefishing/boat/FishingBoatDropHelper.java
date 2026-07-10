package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Shared helpers for converting fishing boat entities into dropped items. */
public final class FishingBoatDropHelper {
  private FishingBoatDropHelper() {}

  public static void dropBoatAsItem(
      @Nonnull Ref<EntityStore> boatRef,
      @Nonnull FishingBoatComponent boat,
      @Nonnull TransformComponent transform,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      boolean shouldDropItem
  ) {
    boat.setSuppressBlockPlacement(true);
    commandBuffer.removeEntity(boatRef, RemoveReason.REMOVE);

    if (!shouldDropItem || boat.getSourceItem() == null || boat.getSourceItem().isEmpty()) {
      return;
    }

    Vector3d dropPosition = transform.getPosition();
    double surfaceY =
        BoatWaterHelper.deckYForPosition(
            commandBuffer.getExternalData().getWorld(), dropPosition.x, dropPosition.z
        );
    if (!Double.isNaN(surfaceY)) {
      dropPosition = new Vector3d(dropPosition.x, surfaceY, dropPosition.z);
    } else {
      dropPosition = dropPosition.add(new Vector3d(0.0, 0.5, 0.0));
    }

    var drop =
        ItemComponent.generateItemDrop(
            commandBuffer,
            new ItemStack(boat.getSourceItem()),
            dropPosition,
            transform.getRotation(),
            0.0f,
            1.0f,
            0.0f
        );
    if (drop != null) {
      commandBuffer.addEntity(drop, AddReason.SPAWN);
    }
  }
}
