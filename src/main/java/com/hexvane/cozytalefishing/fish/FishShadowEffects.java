package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingSplashEffects;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class FishShadowEffects {
    private static final String WATER_POKE_SOUND = "SFX_Tool_Watering_Can_Water";
    private static final String LAVA_POKE_SOUND = "SFX_Staff_Flame_Fireball_Impact";

    private FishShadowEffects() {}

    public static void playPoke(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Vector3d position,
        @Nonnull WaterBodyType waterBodyType
    ) {
        playSound(accessor, pokeSoundFor(waterBodyType), position);
        FishingSplashEffects.playRipple(accessor, position, waterBodyType);
    }

    public static void playPullUnder(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Vector3d position,
        @Nonnull WaterBodyType waterBodyType
    ) {
        playSound(accessor, pokeSoundFor(waterBodyType), position);
        FishingSplashEffects.playRipple(accessor, position, waterBodyType);
    }

    public static void playFightSplash(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Vector3d position,
        @Nonnull WaterBodyType waterBodyType
    ) {
        FishingSplashEffects.playBobberSplash(accessor, position, waterBodyType);
    }

    private static String pokeSoundFor(@Nonnull WaterBodyType waterBodyType) {
        return waterBodyType == WaterBodyType.Lava ? LAVA_POKE_SOUND : WATER_POKE_SOUND;
    }

    private static void playSound(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull String soundId, @Nonnull Vector3d position) {
        int soundIdx = SoundEvent.getAssetMap().getIndex(soundId);
        if (soundIdx != 0) {
            SoundUtil.playSoundEvent3d(null, soundIdx, position, accessor);
        }
    }
}
