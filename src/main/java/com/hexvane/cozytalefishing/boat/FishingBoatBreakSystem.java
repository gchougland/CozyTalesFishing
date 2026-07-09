package com.hexvane.cozytalefishing.boat;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Duration;
import java.util.Set;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Breaks fishing boats after repeated hits and drops the source item. */
public final class FishingBoatBreakSystem extends DamageEventSystem {
  private static final Duration HIT_RESET_TIME = Duration.ofSeconds(10);

  @Nonnull
  private final Query<EntityStore> query;
  @Nonnull
  private final ResourceType<EntityStore, TimeResource> timeResourceType;
  @Nonnull
  private final Set<Dependency<EntityStore>> dependencies =
      Set.of(
          new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
          new SystemGroupDependency<>(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
          new SystemGroupDependency<>(Order.BEFORE, DamageModule.get().getInspectDamageGroup())
      );

  public FishingBoatBreakSystem(@Nonnull ResourceType<EntityStore, TimeResource> timeResourceType) {
    this.timeResourceType = timeResourceType;
    this.query = Archetype.of(FishingBoatComponent.getComponentType(), TransformComponent.getComponentType());
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return query;
  }

  @Nonnull
  @Override
  public Set<Dependency<EntityStore>> getDependencies() {
    return dependencies;
  }

  @Override
  public void handle(
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Damage damage
  ) {
    FishingBoatComponent boat = archetypeChunk.getComponent(index, FishingBoatComponent.getComponentType());
    if (boat == null) {
      return;
    }

    var currentTime = commandBuffer.getResource(timeResourceType).getNow();
    if (boat.getLastHit() != null && currentTime.isAfter(boat.getLastHit().plus(HIT_RESET_TIME))) {
      boat.setLastHit(null);
      boat.setNumberOfHits(0);
    }

    if (damage.getAmount() <= 0.0f) {
      return;
    }

    boat.setNumberOfHits(boat.getNumberOfHits() + 1);
    boat.setLastHit(currentTime);

    int requiredHits = Math.max(1, FishingModConfig.get().getBoatBreakHitsRequired());
    if (boat.getNumberOfHits() < requiredHits) {
      return;
    }

    boat.setSuppressBlockPlacement(true);
    commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);

    boolean shouldDropItem = true;
    if (damage.getSource() instanceof Damage.EntitySource source) {
      Player player =
          source.getRef().isValid() ? commandBuffer.getComponent(source.getRef(), Player.getComponentType()) : null;
      if (player != null && player.getGameMode() == GameMode.Creative) {
        shouldDropItem = false;
      }
    }

    if (!shouldDropItem || boat.getSourceItem() == null || boat.getSourceItem().isEmpty()) {
      return;
    }

    TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    Vector3d dropPosition = transform.getPosition();
    double surfaceY = BoatWaterHelper.deckYForPosition(commandBuffer.getExternalData().getWorld(), dropPosition.x, dropPosition.z);
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
