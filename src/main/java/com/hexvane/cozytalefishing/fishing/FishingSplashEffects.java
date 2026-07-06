package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class FishingSplashEffects {
    private static final String WATER_SOUND_EVENT_ID = "SFX_Tool_Watering_Can_Water";

    private FishingSplashEffects() {}

    public static void playBobberSplash(
        @Nonnull ComponentAccessor<EntityStore> entityAccessor,
        @Nonnull Vector3d position
    ) {
        Vector3d splashPos = new Vector3d(position.x, position.y + 0.05, position.z);

        int soundIdx = SoundEvent.getAssetMap().getIndex(WATER_SOUND_EVENT_ID);
        if (soundIdx != 0) {
            SoundUtil.playSoundEvent3d(null, soundIdx, splashPos, entityAccessor);
        }

        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            entityAccessor.getResource(EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(splashPos, ParticleUtil.DEFAULT_PARTICLE_DISTANCE, nearby);
        ParticleUtil.spawnParticleEffect(FishingConstants.SPLASH_PARTICLE_SYSTEM_ID, splashPos, nearby, entityAccessor);
    }
}
