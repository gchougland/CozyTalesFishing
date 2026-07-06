package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class FishingBootstrap {
    private FishingBootstrap() {}

    public static void register(@Nonnull CozyTalesFishingPlugin plugin) {
        FishingLineComponent.register(plugin.getEntityStoreRegistry());
        FishingBobberComponent.register(plugin.getEntityStoreRegistry());

        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyFishingCharge", FishingChargeInteraction.class, FishingChargeInteraction.CODEC);
        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyFishingPrimary", FishingPrimaryInteraction.class, FishingPrimaryInteraction.CODEC);
        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyFishingCast", FishingCastInteraction.class, FishingCastInteraction.CODEC);
        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyFishingRecall", FishingRecallInteraction.class, FishingRecallInteraction.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new FishingLineTickSystem());
        plugin.getEntityStoreRegistry().registerSystem(new FishingBobberFloatSystem());

        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, FishingBootstrap::onPlayerDisconnect);
    }

    private static void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }

        var world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            FishingLineComponent line = store.getComponent(ref, FishingLineComponent.getComponentType());
            if (line != null && line.isActive()) {
                FishingDebugLog.info("Disconnect cleanup: tearing down active line for player");
                FishingLineService.teardownLine(store, ref);
            }
        });
    }

    /** Called when player no longer holds the fishing rod — optional external hook. */
    public static void cleanupIfNeeded(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        if (!store.getArchetype(playerRef).contains(Player.getComponentType())) {
            return;
        }
        FishingLineComponent line = store.getComponent(playerRef, FishingLineComponent.getComponentType());
        if (line != null && line.isActive()) {
            FishingLineService.teardownLine(store, playerRef);
        }
    }
}
