package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.journal.FishCatchCelebrationPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class FishCatchCelebrationService {
    private FishCatchCelebrationService() {}

    public static void scheduleOpen(
        @Nonnull World world,
        @Nonnull PlayerRef playerRef,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull FishSpeciesAsset species,
        float sizeCm,
        @Nonnull FishCatchCelebrationPage.CelebrationType type
    ) {
        String displayName = FishSpeciesDisplayNames.resolve(species);
        String itemId = species.getItemId();
        world.execute(
            () -> {
                if (!ref.isValid()) {
                    return;
                }
                Store<EntityStore> store = ref.getStore();
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    return;
                }
                player
                    .getPageManager()
                    .openCustomPage(
                        ref,
                        store,
                        new FishCatchCelebrationPage(playerRef, itemId, displayName, sizeCm, type)
                    );
            }
        );
    }
}
