package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.server.core.prefab.event.PrefabPlaceEntityEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Prefab exports bake aquarium fish/decoration display props with absolute {@code BlockOrigin} from the export
 * world. Those copies are not owned by the live {@link AquariumBlock}; cancel them so only PlaceSystem-spawned
 * displays (correct origin) remain.
 */
public final class AquariumPrefabDisplayFilterSystem extends WorldEventSystem<EntityStore, PrefabPlaceEntityEvent> {
    public AquariumPrefabDisplayFilterSystem() {
        super(PrefabPlaceEntityEvent.class);
    }

    @Override
    public void handle(
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull PrefabPlaceEntityEvent event
    ) {
        Holder<EntityStore> holder = event.getHolder();
        if (holder.getComponent(AquariumFishDisplayComponent.getComponentType()) != null
            || holder.getComponent(AquariumDecorationDisplayComponent.getComponentType()) != null) {
            event.setCancelled(true);
        }
    }
}
