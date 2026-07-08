package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class FishCatchRecordSync {
    private FishCatchRecordSync() {}

    public static void syncDisplayName(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef playerRef
    ) {
        syncDisplayName(ref, store, playerRef.getUsername());
    }

    /** Defers a display-name write until after the current store tick (safe from UI/interaction callbacks). */
    public static void scheduleDisplayNameSync(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef playerRef
    ) {
        World world = store.getExternalData().getWorld();
        world.execute(
            () -> {
                if (!ref.isValid()) {
                    return;
                }
                syncDisplayName(ref, store, playerRef);
            }
        );
    }

    public static void syncDisplayName(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull PlayerRef playerRef
    ) {
        syncDisplayName(ref, commandBuffer, playerRef.getUsername());
    }

    public static void syncDisplayName(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull String username
    ) {
        FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
        if (records == null) {
            records = new FishCatchRecordComponent();
        }
        if (records.updateDisplayName(username)) {
            store.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
        }
    }

    public static void syncDisplayName(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull String username
    ) {
        FishCatchRecordComponent records = commandBuffer.getComponent(ref, FishCatchRecordComponent.getComponentType());
        if (records == null) {
            records = new FishCatchRecordComponent();
        }
        if (records.updateDisplayName(username)) {
            commandBuffer.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
        }
    }
}
