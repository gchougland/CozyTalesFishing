package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Replaces a parked fishing boat block with a mountable entity and mounts the player. */
public final class MountFishingBoatFromBlockInteraction extends SimpleBlockInteraction {
  public static final BuilderCodec<MountFishingBoatFromBlockInteraction> CODEC =
      BuilderCodec.builder(
              MountFishingBoatFromBlockInteraction.class,
              MountFishingBoatFromBlockInteraction::new,
              SimpleBlockInteraction.CODEC
          )
          .documentation("Spawns a fishing boat entity from a parked block and mounts the player")
          .build();

  @Override
  protected void interactWithBlock(
      @Nonnull World world,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nullable ItemStack itemInHand,
      @Nonnull Vector3i targetBlock,
      @Nonnull CooldownHandler cooldownHandler
  ) {
    if (!FishingBoatBlockHelper.isPlacedBoatBlock(world, targetBlock)) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    BoatWaterHelper.WaterPlacement placement =
        BoatWaterHelper.resolveWaterPlacement(world, targetBlock.x, targetBlock.y, targetBlock.z);
    if (!BoatWaterHelper.isValidPlacement(world, placement)) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    float yaw = FishingBoatBlockHelper.yawFromBlockRotation(world, targetBlock);
    Vector3d spawnPosition =
        new Vector3d(placement.blockX() + 0.5, placement.surfaceY(), placement.blockZ() + 0.5);
    Ref<EntityStore> playerRef = context.getEntity();

    commandBuffer.run(
        store -> {
          if (!FishingBoatBlockHelper.removeBoatBlockKeepingWater(
              world, targetBlock.x, targetBlock.y, targetBlock.z
          )) {
            context.getState().state = InteractionState.Failed;
            return;
          }

          Ref<EntityStore> boatRef =
              FishingBoatSpawner.spawnBoat(
                  store,
                  spawnPosition,
                  yaw,
                  FishingBoatComponent.DEFAULT_SOURCE_ITEM
              );
          if (boatRef == null) {
            FishingBoatBlockHelper.placeParkedBoatBlock(
                world,
                targetBlock.x,
                targetBlock.y,
                targetBlock.z,
                yaw
            );
            context.getState().state = InteractionState.Failed;
            return;
          }

          if (!FishingBoatSpawner.mountPlayerOnBoat(boatRef, playerRef, store, commandBuffer)) {
            FishingBoatComponent boat = store.getComponent(boatRef, FishingBoatComponent.getComponentType());
            if (boat != null) {
              boat.setSuppressBlockPlacement(true);
            }
            commandBuffer.removeEntity(boatRef, RemoveReason.REMOVE);
            FishingBoatBlockHelper.placeParkedBoatBlock(
                world,
                targetBlock.x,
                targetBlock.y,
                targetBlock.z,
                yaw
            );
            context.getState().state = InteractionState.Failed;
          }
        }
    );
  }

  @Override
  protected void simulateInteractWithBlock(
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nullable ItemStack itemInHand,
      @Nonnull World world,
      @Nonnull Vector3i targetBlock
  ) {}
}
