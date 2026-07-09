package com.hexvane.cozytalefishing.boat.npc;

import com.hexvane.cozytalefishing.boat.FishingBoatConstants;
import com.hexvane.cozytalefishing.boat.FishingBoatMovementHelper;
import com.hexvane.cozytalefishing.boat.FishingBoatVelocityHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementConfig;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import com.hexvane.cozytalefishing.boat.npc.builders.BuilderActionCozyBoatMount;
import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Mount action for fishing boats that swaps to a zero-gravity mounted role instead of Empty_Role. */
public final class ActionCozyBoatMount extends ActionBase {
  private final float anchorX;
  private final float anchorY;
  private final float anchorZ;
  private final String movementConfigId;
  private final int mountedRoleIndex;

  public ActionCozyBoatMount(
      @Nonnull BuilderActionCozyBoatMount builder,
      @Nonnull BuilderSupport builderSupport
  ) {
    super(builder);
    this.anchorX = builder.getAnchorX(builderSupport);
    this.anchorY = builder.getAnchorY(builderSupport);
    this.anchorZ = builder.getAnchorZ(builderSupport);
    this.movementConfigId = builder.getMovementConfig(builderSupport);
    this.mountedRoleIndex = NPCPlugin.get().getIndex(FishingBoatConstants.MOUNTED_EMPTY_ROLE_ID);
  }

  @Override
  public boolean canExecute(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Role role,
      @Nullable InfoProvider sensorInfo,
      double dt,
      @Nonnull Store<EntityStore> store
  ) {
    final var target = role.getStateSupport().getInteractionIterationTarget();
    final boolean targetExists =
        target != null && !store.getArchetype(target).contains(DeathComponent.getComponentType());
    return super.canExecute(ref, role, sensorInfo, dt, store) && targetExists;
  }

  @Override
  public boolean execute(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Role role,
      @Nullable InfoProvider sensorInfo,
      double dt,
      @Nonnull Store<EntityStore> store
  ) {
    super.execute(ref, role, sensorInfo, dt, store);

    var mountComponentType = NPCMountComponent.getComponentType();
    if (store.getComponent(ref, mountComponentType) != null) {
      return false;
    }

    var mountComponent = store.ensureAndGetComponent(ref, mountComponentType);
    mountComponent.setOriginalRoleIndex(NPCPlugin.get().getIndex(role.getRoleName()));

    final var playerReference = role.getStateSupport().getInteractionIterationTarget();
    if (playerReference == null) {
      return false;
    }

    final var playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
    if (playerRefComponent == null) {
      return false;
    }

    mountComponent.setOwnerPlayerRef(playerRefComponent);
    mountComponent.setAnchor(anchorX, anchorY, anchorZ);

    final var playerComponent = store.getComponent(playerReference, Player.getComponentType());
    if (playerComponent == null) {
      return false;
    }

    final var playerPhysicsValues = store.getComponent(playerReference, PhysicsValues.getComponentType());
    if (mountedRoleIndex < 0) {
      return false;
    }

    RoleChangeSystem.requestRoleChange(ref, role, mountedRoleIndex, false, null, null, store);

    FishingBoatVelocityHelper.zeroVelocity(ref, store);
    FishingBoatVelocityHelper.zeroVelocity(playerReference, store);

    final var movementConfig = MovementConfig.getAssetMap().getAsset(movementConfigId);
    if (movementConfig != null) {
      final var movementManagerComponent = store.getComponent(playerReference, MovementManager.getComponentType());
      if (movementManagerComponent != null && playerPhysicsValues != null) {
        FishingBoatMovementHelper.applyBoatMountMovement(
            movementManagerComponent,
            movementConfig,
            playerPhysicsValues,
            playerComponent.getGameMode(),
            playerRefComponent.getPacketHandler()
        );
      }
    }

    return true;
  }
}
