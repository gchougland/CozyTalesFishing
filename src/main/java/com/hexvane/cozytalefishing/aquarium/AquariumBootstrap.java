package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import javax.annotation.Nonnull;

public final class AquariumBootstrap {
    private AquariumBootstrap() {}

    public static void register(@Nonnull CozyTalesFishingPlugin plugin) {
        AssetRegistry.register(
            HytaleAssetStore.builder(AquariumLayoutAsset.class, new DefaultAssetMap<>())
                .setPath("CozyTalesFishing/Aquarium/Layouts")
                .setCodec(AquariumLayoutAsset.CODEC)
                .setKeyFunction(AquariumLayoutAsset::getId)
                .build()
        );

        AssetRegistry.register(
            HytaleAssetStore.builder(AquariumDecorationAsset.class, new DefaultAssetMap<>())
                .setPath("CozyTalesFishing/Aquarium/Decorations")
                .setCodec(AquariumDecorationAsset.CODEC)
                .setKeyFunction(AquariumDecorationAsset::getId)
                .loadsAfter(Item.class)
                .build()
        );

        plugin
            .getEventRegistry()
            .register(LoadedAssetsEvent.class, AquariumLayoutAsset.class, AquariumBootstrap::onLayoutsLoaded);
        plugin
            .getEventRegistry()
            .register(LoadedAssetsEvent.class, AquariumDecorationAsset.class, AquariumBootstrap::onDecorationsLoaded);

        var aquariumBlockType =
            plugin.getChunkStoreRegistry().registerComponent(AquariumBlock.class, "AquariumBlock", AquariumBlock.CODEC);
        AquariumBlock.register(aquariumBlockType);

        var displayComponentType =
            plugin
                .getEntityStoreRegistry()
                .registerComponent(
                    AquariumFishDisplayComponent.class,
                    "CozyAquariumFishDisplay",
                    AquariumFishDisplayComponent.CODEC
                );
        AquariumFishDisplayComponent.register(displayComponentType);

        var decorationDisplayComponentType =
            plugin
                .getEntityStoreRegistry()
                .registerComponent(
                    AquariumDecorationDisplayComponent.class,
                    "CozyAquariumDecorationDisplay",
                    AquariumDecorationDisplayComponent.CODEC
                );
        AquariumDecorationDisplayComponent.register(decorationDisplayComponentType);

        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyAquariumUse", AquariumUseInteraction.class, AquariumUseInteraction.CODEC);

        FluidTicker.CODEC.register("Static", StaticFluidTicker.class, StaticFluidTicker.CODEC);

        var chunkStoreRegistry = plugin.getChunkStoreRegistry();
        chunkStoreRegistry.registerSystem(new AquariumPlaceSystem());
        chunkStoreRegistry.registerSystem(new AquariumBreakSystem());
        chunkStoreRegistry.registerSystem(new AquariumDisplayHeartbeatSystem());

        AquariumLayoutRegistry.rebuild();
        AquariumDecorationRegistry.rebuild();
    }

    private static void onLayoutsLoaded(
        @Nonnull LoadedAssetsEvent<String, AquariumLayoutAsset, DefaultAssetMap<String, AquariumLayoutAsset>> event
    ) {
        AquariumLayoutRegistry.rebuild();
    }

    private static void onDecorationsLoaded(
        @Nonnull LoadedAssetsEvent<String, AquariumDecorationAsset, DefaultAssetMap<String, AquariumDecorationAsset>> event
    ) {
        AquariumDecorationRegistry.rebuild();
    }
}
