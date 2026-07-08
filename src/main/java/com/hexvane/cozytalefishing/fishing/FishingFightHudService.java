package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Shows and updates the fishing fight stamina HUD without touching the entity store. */
public final class FishingFightHudService {
    private static final long UPDATE_INTERVAL_NANOS = 50_000_000L;

    private static final Map<UUID, Long> lastUpdateNanos = new ConcurrentHashMap<>();

    private FishingFightHudService() {}

    public static void show(@Nonnull Player player, @Nonnull PlayerRef playerRef, float maxStamina) {
        FishingFightStaminaHud hud = new FishingFightStaminaHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, hud);
        hud.updateStamina(maxStamina, maxStamina);
        lastUpdateNanos.put(playerRef.getUuid(), System.nanoTime());
    }

    public static void update(@Nonnull Player player, @Nonnull PlayerRef playerRef, float current, float max) {
        long now = System.nanoTime();
        Long last = lastUpdateNanos.get(playerRef.getUuid());
        if (last != null && now - last < UPDATE_INTERVAL_NANOS) {
            return;
        }
        lastUpdateNanos.put(playerRef.getUuid(), now);

        CustomUIHud hud = player.getHudManager().getCustomHud(FishingFightStaminaHud.KEY);
        if (hud instanceof FishingFightStaminaHud fightHud) {
            fightHud.updateStamina(current, max);
        }
    }

    public static void hide(@Nullable Player player, @Nullable PlayerRef playerRef) {
        if (playerRef != null) {
            lastUpdateNanos.remove(playerRef.getUuid());
        }
        if (player == null || playerRef == null) {
            return;
        }
        if (player.getHudManager().getCustomHud(FishingFightStaminaHud.KEY) != null) {
            player.getHudManager().removeCustomHud(playerRef, FishingFightStaminaHud.KEY);
        }
    }
}
