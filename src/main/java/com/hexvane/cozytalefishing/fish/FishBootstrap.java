package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hexvane.cozytalefishing.fishing.FishingReelInteraction;
import com.hexvane.cozytalefishing.treasure.OpenMessageInBottleInteraction;
import com.hexvane.cozytalefishing.treasure.OpenSunkenTreasureInteraction;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hexvane.cozytalefishing.journal.FishingJournalConstants;
import com.hexvane.cozytalefishing.journal.FishingJournalPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class FishBootstrap {
    private FishBootstrap() {}

    public static void register(@Nonnull CozyTalesFishingPlugin plugin) {
        AssetRegistry.register(
            HytaleAssetStore.builder(FishSpeciesAsset.class, new DefaultAssetMap<>())
                .setPath("CozyTalesFishing/Fish")
                .setCodec(FishSpeciesAsset.CODEC)
                .setKeyFunction(FishSpeciesAsset::getId)
                .loadsAfter(Environment.class, Item.class)
                .build()
        );

        AssetRegistry.register(
            HytaleAssetStore.builder(WaterBodyBiomeOverridesAsset.class, new DefaultAssetMap<>())
                .setPath("CozyTalesFishing/Config/WaterBodyBiomeOverrides")
                .setCodec(WaterBodyBiomeOverridesAsset.CODEC)
                .setKeyFunction(WaterBodyBiomeOverridesAsset::getId)
                .build()
        );

        FishShadowComponent.register(plugin.getEntityStoreRegistry());
        FishCatchRecordComponent.register(plugin.getEntityStoreRegistry());
        FishShadowSpawnStateComponent.register(plugin.getEntityStoreRegistry());

        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyFishingReel", FishingReelInteraction.class, FishingReelInteraction.CODEC);
        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyOpenSunkenTreasure", OpenSunkenTreasureInteraction.class, OpenSunkenTreasureInteraction.CODEC);
        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyOpenMessageInBottle", OpenMessageInBottleInteraction.class, OpenMessageInBottleInteraction.CODEC);

        OpenCustomUIInteraction.registerSimple(
            plugin,
            FishingJournalPage.class,
            FishingJournalConstants.PAGE_ID,
            FishingJournalPage::new
        );

        plugin.getEntityStoreRegistry().registerSystem(new FishShadowSpawnSystem());
        plugin.getEntityStoreRegistry().registerSystem(new FishShadowTickSystem());

        plugin.getCommandRegistry().registerCommand(new CozyFishCommand());

        plugin
            .getEventRegistry()
            .register(LoadedAssetsEvent.class, FishSpeciesAsset.class, FishBootstrap::onFishSpeciesLoaded);
        plugin
            .getEventRegistry()
            .register(LoadedAssetsEvent.class, WaterBodyBiomeOverridesAsset.class, FishBootstrap::onBiomeOverridesLoaded);

        plugin.getEventRegistry().registerGlobal(BootEvent.class, event -> FishShadowCleanupService.scheduleAllWorldsCleanup());
        plugin.getEventRegistry().registerGlobal(StartWorldEvent.class, FishBootstrap::onWorldStart);
        plugin.getEventRegistry().registerGlobal(AddWorldEvent.class, FishBootstrap::onWorldAdded);
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, FishBootstrap::onPlayerAddedToWorld);

        FishSpeciesRegistry.rebuild();
    }

    private static void onWorldStart(@Nonnull StartWorldEvent event) {
        FishShadowCleanupService.scheduleWorldStartCleanup(event.getWorld());
    }

    private static void onWorldAdded(@Nonnull AddWorldEvent event) {
        FishShadowCleanupService.scheduleWorldStartCleanup(event.getWorld());
    }

    private static void onPlayerAddedToWorld(@Nonnull AddPlayerToWorldEvent event) {
        var playerRef = event.getHolder().getComponent(com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        event.getWorld().execute(() -> FishCatchRecordSync.syncDisplayName(ref, store, playerRef));
    }

    /** Clears shadows left in already-loaded worlds when the plugin starts. */
    public static void cleanupLoadedWorlds() {
        FishShadowCleanupService.scheduleAllWorldsCleanup();
    }

    private static void onFishSpeciesLoaded(
        @Nonnull LoadedAssetsEvent<String, FishSpeciesAsset, DefaultAssetMap<String, FishSpeciesAsset>> event
    ) {
        FishSpeciesRegistry.rebuild();
    }

    private static void onBiomeOverridesLoaded(
        @Nonnull LoadedAssetsEvent<String, WaterBodyBiomeOverridesAsset, DefaultAssetMap<String, WaterBodyBiomeOverridesAsset>> event
    ) {
        WaterBodyClassifier.invalidateCaches();
    }
}
