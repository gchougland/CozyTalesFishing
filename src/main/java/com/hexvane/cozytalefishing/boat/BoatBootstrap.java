package com.hexvane.cozytalefishing.boat;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hexvane.cozytalefishing.boat.npc.builders.BuilderActionCozyBoatMount;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import javax.annotation.Nonnull;

public final class BoatBootstrap {
  private BoatBootstrap() {}

  public static void register(@Nonnull CozyTalesFishingPlugin plugin) {
    FishingBoatComponent.register(plugin.getEntityStoreRegistry());
    NPCPlugin.get().registerCoreComponentType("CozyBoatMount", BuilderActionCozyBoatMount::new);

    plugin
        .getCodecRegistry(Interaction.CODEC)
        .register("CozySpawnFishingBoat", SpawnFishingBoatInteraction.class, SpawnFishingBoatInteraction.CODEC);
    plugin
        .getCodecRegistry(Interaction.CODEC)
        .register(
            "CozyMountFishingBoat",
            MountFishingBoatFromBlockInteraction.class,
            MountFishingBoatFromBlockInteraction.CODEC
        );

    var entityStoreRegistry = plugin.getEntityStoreRegistry();
    entityStoreRegistry.registerSystem(new FishingBoatEntitySetup());
    entityStoreRegistry.registerSystem(new FishingBoatDriftStopSystem());
    entityStoreRegistry.registerSystem(new FishingBoatItemFloatSystem());
    entityStoreRegistry.registerSystem(new FishingBoatDismountToBlockSystem());
    entityStoreRegistry.registerSystem(new FishingBoatMountViewBobbingSystem.OnMountChange());
    entityStoreRegistry.registerSystem(new FishingBoatMountViewBobbingSystem.OnBoatRemoved());
    entityStoreRegistry.registerSystem(new FishingBoatBreakSystem(TimeResource.getResourceType()));
  }
}
