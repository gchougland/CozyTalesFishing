package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.WaterBodyType;
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
    private static final String LAVA_SOUND_EVENT_ID = "SFX_Staff_Flame_Fireball_Impact";

    private FishingSplashEffects() {}

    public static void playBobberSplash(
        @Nonnull ComponentAccessor<EntityStore> entityAccessor,
        @Nonnull Vector3d position,
        @Nonnull WaterBodyType waterBodyType
    ) {
        boolean lava = waterBodyType == WaterBodyType.Lava;
        Vector3d splashPos = new Vector3d(position.x, position.y + (lava ? 0.0 : 0.05), position.z);

        String soundId = lava ? LAVA_SOUND_EVENT_ID : WATER_SOUND_EVENT_ID;
        int soundIdx = SoundEvent.getAssetMap().getIndex(soundId);
        if (soundIdx != 0) {
            SoundUtil.playSoundEvent3d(null, soundIdx, splashPos, entityAccessor);
        }

        String particleId =
            lava ? FishingConstants.LAVA_SPLASH_PARTICLE_SYSTEM_ID : FishingConstants.SPLASH_PARTICLE_SYSTEM_ID;
        spawnParticles(entityAccessor, splashPos, particleId);
    }

    public static void playRipple(
        @Nonnull ComponentAccessor<EntityStore> entityAccessor,
        @Nonnull Vector3d position,
        @Nonnull WaterBodyType waterBodyType
    ) {
        boolean lava = waterBodyType == WaterBodyType.Lava;
        Vector3d ripplePos = new Vector3d(position.x, position.y + (lava ? 0.0 : 0.02), position.z);
        String particleId =
            lava ? FishingConstants.LAVA_RIPPLE_PARTICLE_SYSTEM_ID : FishingConstants.RIPPLE_PARTICLE_SYSTEM_ID;
        spawnParticles(entityAccessor, ripplePos, particleId);
    }

    private static void spawnParticles(
        @Nonnull ComponentAccessor<EntityStore> entityAccessor,
        @Nonnull Vector3d position,
        @Nonnull String particleSystemId
    ) {
        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            entityAccessor.getResource(EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(position, ParticleUtil.DEFAULT_PARTICLE_DISTANCE, nearby);
        ParticleUtil.spawnParticleEffect(particleSystemId, position, nearby, entityAccessor);
    }
}
