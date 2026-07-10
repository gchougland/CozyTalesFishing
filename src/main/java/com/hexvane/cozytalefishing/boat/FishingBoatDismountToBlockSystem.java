package com.hexvane.cozytalefishing.boat;



import com.hypixel.hytale.builtin.mounts.NPCMountComponent;

import com.hypixel.hytale.component.CommandBuffer;

import com.hypixel.hytale.component.ComponentType;

import com.hypixel.hytale.component.Ref;

import com.hypixel.hytale.component.RemoveReason;

import com.hypixel.hytale.component.Store;

import com.hypixel.hytale.component.query.Query;

import com.hypixel.hytale.component.system.RefChangeSystem;

import com.hypixel.hytale.math.vector.Rotation3f;

import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import com.hypixel.hytale.server.core.universe.world.World;

import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import org.joml.Vector3d;



/** Converts a dismounted fishing boat entity into a parked block above the water surface. */

public final class FishingBoatDismountToBlockSystem extends RefChangeSystem<EntityStore, NPCMountComponent> {

  private static final int PLACEMENT_SEARCH_RADIUS = 2;



  @Nonnull

  @Override

  public ComponentType<EntityStore, NPCMountComponent> componentType() {

    return NPCMountComponent.getComponentType();

  }



  @Nonnull

  @Override

  public Query<EntityStore> getQuery() {

    return FishingBoatComponent.getComponentType();

  }



  @Override

  public void onComponentAdded(

      @Nonnull Ref<EntityStore> ref,

      @Nonnull NPCMountComponent component,

      @Nonnull Store<EntityStore> store,

      @Nonnull CommandBuffer<EntityStore> commandBuffer

  ) {}



  @Override

  public void onComponentSet(

      @Nonnull Ref<EntityStore> ref,

      @Nullable NPCMountComponent oldComponent,

      @Nonnull NPCMountComponent newComponent,

      @Nonnull Store<EntityStore> store,

      @Nonnull CommandBuffer<EntityStore> commandBuffer

  ) {}



  @Override

  public void onComponentRemoved(

      @Nonnull Ref<EntityStore> ref,

      @Nonnull NPCMountComponent component,

      @Nonnull Store<EntityStore> store,

      @Nonnull CommandBuffer<EntityStore> commandBuffer

  ) {
    FishingBoatComponent boat = store.getComponent(ref, FishingBoatComponent.getComponentType());
    if (boat == null || boat.isSuppressBlockPlacement()) {
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    PlayerRef ownerPlayerRef = component.getOwnerPlayerRef();

    FishingBoatVelocityHelper.zeroVelocity(ref, store);

    World world = store.getExternalData().getWorld();

    Vector3d pos = transform.getPosition();

    int originBlockX = (int) Math.floor(pos.x);

    int originBlockZ = (int) Math.floor(pos.z);

    float yaw = FishingBoatBlockHelper.entityYawToParkedBlockYaw(transform.getRotation().yaw());

    int[] placement =
        FishingBoatBlockHelper.findNearbyParkedBoatPlacement(
            world, originBlockX, originBlockZ, yaw, PLACEMENT_SEARCH_RADIUS
        );

    commandBuffer.run(
        _store -> {
          if (placement != null) {
            int blockX = placement[0];
            int parkedY = placement[1];
            int blockZ = placement[2];
            Vector3d standPosition =
                FishingBoatBlockHelper.standPositionOnParkedBlock(blockX, parkedY, blockZ);

            if (!FishingBoatBlockHelper.placeParkedBoatBlock(world, blockX, parkedY, blockZ, yaw)) {
              return;
            }

            Ref<EntityStore> playerRef = ownerPlayerRef != null ? ownerPlayerRef.getReference() : null;
            if (playerRef != null && playerRef.isValid()) {
              TransformComponent playerTransform =
                  _store.getComponent(playerRef, TransformComponent.getComponentType());
              Rotation3f standRotation =
                  playerTransform != null ? playerTransform.getRotation() : transform.getRotation();
              commandBuffer.addComponent(
                  playerRef,
                  Teleport.getComponentType(),
                  Teleport.createForPlayer(standPosition, standRotation)
              );
            }

            boat.setSuppressBlockPlacement(true);
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            return;
          }

          FishingBoatDropHelper.dropBoatAsItem(ref, boat, transform, commandBuffer, true);
        }
    );

  }

}

