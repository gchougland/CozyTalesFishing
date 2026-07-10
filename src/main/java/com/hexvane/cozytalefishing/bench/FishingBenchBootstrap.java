package com.hexvane.cozytalefishing.bench;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import javax.annotation.Nonnull;

public final class FishingBenchBootstrap {
    private FishingBenchBootstrap() {}

    public static void register(@Nonnull CozyTalesFishingPlugin plugin) {
        plugin
            .getCodecRegistry(Interaction.CODEC)
            .register("CozyOpenFishingBench", OpenFishingBenchInteraction.class, OpenFishingBenchInteraction.CODEC);
    }
}
