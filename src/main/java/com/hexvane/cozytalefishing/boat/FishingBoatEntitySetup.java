package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

/** Ensures fishing boat entities have components required for mount role changes. */
public final class FishingBoatEntitySetup extends HolderSystem<EntityStore> {
  @Nonnull
  private final Query<EntityStore> query = FishingBoatComponent.getComponentType();

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return query;
  }

  @Nonnull
  @Override
  public Set<Dependency<EntityStore>> getDependencies() {
    return RootDependency.firstSet();
  }

  @Override
  public void onEntityAdd(
      @Nonnull Holder<EntityStore> holder,
      @Nonnull AddReason reason,
      @Nonnull Store<EntityStore> store
  ) {
    holder.ensureComponent(Interactable.getComponentType());
  }

  @Override
  public void onEntityRemoved(
      @Nonnull Holder<EntityStore> holder,
      @Nonnull RemoveReason reason,
      @Nonnull Store<EntityStore> store
  ) {}
}
