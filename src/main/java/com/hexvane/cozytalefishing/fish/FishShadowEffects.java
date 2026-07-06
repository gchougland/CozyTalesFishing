package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingConstants;
import com.hexvane.cozytalefishing.fishing.FishingSplashEffects;
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

public final class FishShadowEffects {
    private static final String POKE_SOUND = "SFX_Tool_Watering_Can_Water";
    private static final String PULL_SOUND = "SFX_Tool_Watering_Can_Water";

    private FishShadowEffects() {}

    public static void playPoke(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Vector3d position) {
        playSound(accessor, POKE_SOUND, position);
        spawnRipple(accessor, position);
    }

    public static void playPullUnder(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Vector3d position) {
        playSound(accessor, PULL_SOUND, position);
        spawnRipple(accessor, position);
    }

    public static void playFightSplash(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Vector3d position) {
        FishingSplashEffects.playBobberSplash(accessor, position);
    }

    private static void playSound(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull String soundId, @Nonnull Vector3d position) {
        int soundIdx = SoundEvent.getAssetMap().getIndex(soundId);
        if (soundIdx != 0) {
            SoundUtil.playSoundEvent3d(null, soundIdx, position, accessor);
        }
    }

    private static void spawnRipple(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Vector3d position) {
        Vector3d ripplePos = new Vector3d(position.x, position.y + 0.02, position.z);
        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            accessor.getResource(EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(ripplePos, ParticleUtil.DEFAULT_PARTICLE_DISTANCE, nearby);
        ParticleUtil.spawnParticleEffect(FishingConstants.RIPPLE_PARTICLE_SYSTEM_ID, ripplePos, nearby, accessor);
    }
}
