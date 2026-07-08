package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Radial fishing stamina ring centered on the crosshair during fish fights. */
public final class FishingFightStaminaHud extends CustomUIHud {
    public static final String KEY = "cozytalefishing:fight_stamina";

    public FishingFightStaminaHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY, 10);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("CozyTalesFishing/FishingFightStamina.ui");
    }

    public void updateStamina(float current, float max) {
        float normalized = max > 0.0f ? Math.max(0.0f, Math.min(1.0f, current / max)) : 0.0f;

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#StaminaProgress.Value", normalized);
        update(false, commandBuilder);
    }
}
