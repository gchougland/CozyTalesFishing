package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class FishingSpawnRegionSelectionUtil {
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    private FishingSpawnRegionSelectionUtil() {}

    @Nullable
    public static Bounds readBuilderSelection(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        if (BuilderToolsPlugin.get() == null) {
            return null;
        }
        var builderState = BuilderToolsPlugin.getState(player, playerRef);
        if (builderState == null) {
            return null;
        }
        var selection = builderState.getSelection();
        if (selection == null || !selection.hasSelectionBounds()) {
            return null;
        }
        Vector3i min = Vector3iUtil.min(selection.getSelectionMin(), selection.getSelectionMax());
        Vector3i max = Vector3iUtil.max(selection.getSelectionMin(), selection.getSelectionMax());
        // Builder environment uses exclusive max; convert to inclusive for region storage.
        return new Bounds(min.x, min.y, min.z, max.x - 1, max.y - 1, max.z - 1);
    }
}
