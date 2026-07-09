package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Suppresses mount camera bob for fishing boat riders and restores vanilla mount bob on dismount.
 */
public final class FishingBoatMountViewBobbingSystem {
  private FishingBoatMountViewBobbingSystem() {}

  /** Applies / clears view-bobbing overrides when a player mounts or dismounts a fishing boat. */
  public static final class OnMountChange extends RefChangeSystem<EntityStore, NPCMountComponent> {
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
    ) {
      applyForOwner(component, true);
      PlayerRef owner = component.getOwnerPlayerRef();
      if (owner != null) {
        // Re-apply after mount packets so the client keeps the boat-specific override.
        commandBuffer.run(_store -> FishingBoatMountViewBobbingHelper.disableForPlayer(owner));
      }
    }

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
      applyForOwner(component, false);
    }

    private static void applyForOwner(@Nonnull NPCMountComponent component, boolean disableBobbing) {
      PlayerRef owner = component.getOwnerPlayerRef();
      if (owner == null) {
        return;
      }

      if (disableBobbing) {
        FishingBoatMountViewBobbingHelper.disableForPlayer(owner);
      } else {
        FishingBoatMountViewBobbingHelper.restoreForPlayer(owner);
      }
    }
  }

  /** Restores view bobbing if a mounted fishing boat entity is removed without a clean dismount. */
  public static final class OnBoatRemoved extends RefSystem<EntityStore> {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
      return FishingBoatComponent.getComponentType();
    }

    @Override
    public void onEntityAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull AddReason reason,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {}

    @Override
    public void onEntityRemove(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull RemoveReason reason,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
      NPCMountComponent mount = store.getComponent(ref, NPCMountComponent.getComponentType());
      if (mount == null) {
        return;
      }

      PlayerRef owner = mount.getOwnerPlayerRef();
      if (owner != null) {
        FishingBoatMountViewBobbingHelper.restoreForPlayer(owner);
      }
    }
  }
}
