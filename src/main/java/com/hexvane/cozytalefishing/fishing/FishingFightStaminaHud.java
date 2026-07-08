package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Vertical fishing stamina bar shown to the right of the crosshair during fish fights. */
public final class FishingFightStaminaHud extends CustomUIHud {
    public static final String KEY = "cozytalefishing:fight_stamina";
    private static final int FILL_WIDTH = 20;
    private static final int FILL_MAX_HEIGHT = 76;

    public FishingFightStaminaHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY, 10);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("CozyTalesFishing/FishingFightStamina.ui");
    }

    public void updateStamina(float current, float max) {
        float normalized = max > 0.0f ? Math.max(0.0f, Math.min(1.0f, current / max)) : 0.0f;
        int fillHeight = Math.round(FILL_MAX_HEIGHT * normalized);

        Anchor fillAnchor = new Anchor();
        fillAnchor.setWidth(Value.of(FILL_WIDTH));
        fillAnchor.setHeight(Value.of(fillHeight));

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.setObject("#StaminaFill.Anchor", fillAnchor);
        commandBuilder.set("#StaminaText.Text", String.format("%.0f", current));
        update(false, commandBuilder);
    }
}
