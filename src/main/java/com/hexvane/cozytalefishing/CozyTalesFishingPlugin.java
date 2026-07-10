package com.hexvane.cozytalefishing;

import com.hexvane.cozytalefishing.fishing.FishingBootstrap;
import com.hexvane.cozytalefishing.fish.FishBootstrap;
import com.hexvane.cozytalefishing.boat.BoatBootstrap;
import com.hexvane.cozytalefishing.aquarium.AquariumBootstrap;
import com.hexvane.cozytalefishing.generated.HstatsBuildMetadata;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hexvane.cozytalefishing.fish.FishingSpawnRegionRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import java.nio.file.Files;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CozyTalesFishingPlugin extends JavaPlugin {
    private static CozyTalesFishingPlugin instance;

    @Nonnull
    private final Config<FishingModConfig> fishingModConfig = withConfig("FishingModConfig", FishingModConfig.CODEC);

    public CozyTalesFishingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Nullable
    public static CozyTalesFishingPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;
        FishingModConfig.bind(fishingModConfig.get());

        String hstatsModUuid = HstatsBuildMetadata.HSTATS_MOD_UUID;
        String modVersion = this.getManifest().getVersion().toString();
        if (!hstatsModUuid.isBlank()) {
            new HStats(hstatsModUuid, modVersion);
            getLogger().atInfo().log("HStats metrics enabled for CozyTalesFishing v%s.", modVersion);
        } else {
            getLogger()
                .atInfo()
                .log(
                    "HStats metrics disabled: set COZYTALESFISHING_HSTATS_MOD_UUID when building, "
                        + "or Gradle property hstats_mod_uuid, to your hstats.dev mod UUID."
                );
        }

        FishingBootstrap.register(this);
        FishBootstrap.register(this);
        BoatBootstrap.register(this);
        AquariumBootstrap.register(this);

        getLogger().atInfo().log("CozyTalesFishing v%s loaded.", modVersion);
    }

    @Override
    protected void start() {
        var configPath = getDataDirectory().resolve("FishingModConfig.json");
        if (!Files.exists(configPath)) {
            fishingModConfig.save();
            getLogger().atInfo().log("Created default FishingModConfig at %s", configPath);
        }

        FishBootstrap.cleanupLoadedWorlds();
        FishingModConfig.bind(fishingModConfig.get());
        FishingSpawnRegionRegistry.initialize(getDataDirectory());

        if (!this.getManifest().includesAssetPack()) {
            return;
        }

        String packId = new PluginIdentifier(this.getManifest()).toString();
        AssetPack pack = AssetModule.get().getAssetPack(packId);
        if (pack == null) {
            getLogger().atWarning().log("Asset pack %s not found in AssetModule; Asset Editor may not list this mod", packId);
            return;
        }

        HytaleServer.get()
            .getEventBus()
            .<Void, AssetPackRegisterEvent>dispatchFor(AssetPackRegisterEvent.class)
            .dispatch(new AssetPackRegisterEvent(pack));

        if (Universe.get().getPlayerCount() > 0) {
            Universe.get().broadcastPacketNoCache(new RequestCommonAssetsRebuild());
        }
    }

    /** Reloads {@code FishingModConfig.json} from disk and applies it without restarting. */
    public void reloadFishingModConfig() {
        fishingModConfig.load().join();
        FishingModConfig.bind(fishingModConfig.get());
        getLogger().atInfo().log("Reloaded FishingModConfig from %s", getDataDirectory().resolve("FishingModConfig.json"));
    }
}
