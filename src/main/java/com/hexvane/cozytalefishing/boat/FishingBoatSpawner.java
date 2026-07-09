package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementConfig;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Shared fishing boat entity spawn and programmatic mount helpers. */
public final class FishingBoatSpawner {
  private static final float MOUNT_ANCHOR_X = 0.0f;
  private static final float MOUNT_ANCHOR_Y = 1.0f;
  private static final float MOUNT_ANCHOR_Z = 0.3f;

  private FishingBoatSpawner() {}

  @Nullable
  public static Ref<EntityStore> spawnBoat(
      @Nonnull Store<EntityStore> store,
      @Nonnull Vector3d position,
      float yawRadians,
      @Nonnull String sourceItem
  ) {
    int roleIndex = NPCPlugin.get().getIndex(FishingBoatConstants.NPC_ROLE_ID);
    if (roleIndex < 0) {
      return null;
    }

    Rotation3f rotation = new Rotation3f();
    rotation.setYaw(yawRadians);

    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(FishingBoatConstants.MODEL_ID);
    Model spawnModel = modelAsset != null ? Model.createRandomScaleModel(modelAsset) : null;

    var spawned =
        NPCPlugin
            .get()
            .spawnEntity(
                store,
                roleIndex,
                position,
                rotation,
                spawnModel,
                (npc, holder, entityStore) ->
                    holder.addComponent(
                        FishingBoatComponent.getComponentType(),
                        new FishingBoatComponent(sourceItem)
                    ),
                null
            );
    return spawned != null ? spawned.first() : null;
  }

  public static boolean mountPlayerOnBoat(
      @Nonnull Ref<EntityStore> boatRef,
      @Nonnull Ref<EntityStore> playerRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer
  ) {
    if (store.getComponent(boatRef, NPCMountComponent.getComponentType()) != null) {
      return false;
    }

    NPCEntity npcEntity = store.getComponent(boatRef, NPCEntity.getComponentType());
    PlayerRef ownerPlayerRef = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
    Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
    if (npcEntity == null || ownerPlayerRef == null || player == null) {
      return false;
    }

    int originalRoleIndex = NPCPlugin.get().getIndex(FishingBoatConstants.NPC_ROLE_ID);
    if (originalRoleIndex < 0) {
      return false;
    }

    NPCMountComponent mountComponent = commandBuffer.ensureAndGetComponent(boatRef, NPCMountComponent.getComponentType());
    mountComponent.setOriginalRoleIndex(originalRoleIndex);
    mountComponent.setOwnerPlayerRef(ownerPlayerRef);
    mountComponent.setAnchor(MOUNT_ANCHOR_X, MOUNT_ANCHOR_Y, MOUNT_ANCHOR_Z);

    commandBuffer.ensureComponent(boatRef, Interactable.getComponentType());

    int mountedRoleIndex = NPCPlugin.get().getIndex(FishingBoatConstants.MOUNTED_EMPTY_ROLE_ID);
    if (mountedRoleIndex < 0) {
      return false;
    }
    RoleChangeSystem.requestRoleChange(boatRef, npcEntity.getRole(), mountedRoleIndex, false, null, null, store);

    FishingBoatVelocityHelper.zeroVelocity(boatRef, commandBuffer);
    FishingBoatVelocityHelper.zeroVelocity(playerRef, commandBuffer);

    MovementConfig movementConfig = MovementConfig.getAssetMap().getAsset(FishingBoatConstants.MOUNT_CONFIG_ID);
    if (movementConfig != null) {
      PhysicsValues physicsValues = commandBuffer.getComponent(playerRef, PhysicsValues.getComponentType());
      MovementManager movementManager = commandBuffer.getComponent(playerRef, MovementManager.getComponentType());
      if (movementManager != null && physicsValues != null) {
        FishingBoatMovementHelper.applyBoatMountMovement(
            movementManager,
            movementConfig,
            physicsValues,
            player.getGameMode(),
            ownerPlayerRef.getPacketHandler()
        );
      }
    }

    // NPCMountSystems.OnAdd removes Interactable after the role-change rebuild.
    return true;
  }
}
