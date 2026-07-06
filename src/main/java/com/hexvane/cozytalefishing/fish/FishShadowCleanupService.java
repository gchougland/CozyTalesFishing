package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/** Removes fish shadow entities that must not survive world reloads. */
public final class FishShadowCleanupService {
    private static final int CLEANUP_PASSES = 12;

    private FishShadowCleanupService() {}

    public static void scheduleWorldStartCleanup(@Nonnull World world) {
        scheduleCleanupPass(world, 0);
    }

    private static void scheduleCleanupPass(@Nonnull World world, int pass) {
        world.execute(
            () -> {
                despawnAllInWorld(world);
                if (pass + 1 < CLEANUP_PASSES) {
                    scheduleCleanupPass(world, pass + 1);
                }
            }
        );
    }

    public static void scheduleAllWorldsCleanup() {
        for (World world : Universe.get().getWorlds().values()) {
            scheduleWorldStartCleanup(world);
        }
    }

    public static int despawnAllInWorld(@Nonnull World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        return despawnAll(store);
    }

    public static int despawnAll(@Nonnull Store<EntityStore> store) {
        Set<Ref<EntityStore>> refs = new HashSet<>();
        store.forEachEntityParallel(
            FishShadowComponent.getComponentType(),
            (index, chunk, ignored) -> {
                synchronized (refs) {
                    refs.add(chunk.getReferenceTo(index));
                }
            }
        );
        store.forEachEntityParallel(
            PersistentModel.getComponentType(),
            (index, chunk, ignored) -> {
                PersistentModel persistentModel = chunk.getComponent(index, PersistentModel.getComponentType());
                if (persistentModel == null) {
                    return;
                }
                var reference = persistentModel.getModelReference();
                if (reference == null || !FishShadowType.isFishShadowModelId(reference.getModelAssetId())) {
                    return;
                }
                synchronized (refs) {
                    refs.add(chunk.getReferenceTo(index));
                }
            }
        );
        int removed = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) {
                store.removeEntity(ref, RemoveReason.REMOVE);
                removed++;
            }
        }
        return removed;
    }
}
