package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Local-space offset applied to fish display props inside aquariums. */
public final class AquariumDisplayOffset {
    private AquariumDisplayOffset() {}

    @Nonnull
    public static Vector3d apply(@Nonnull Vector3d basePosition, @Nullable float[] offset, int rotationIndex) {
        if (offset == null || offset.length == 0) {
            return basePosition;
        }

        float offsetX = offset.length > 0 ? offset[0] : 0.0f;
        float offsetY = offset.length > 1 ? offset[1] : 0.0f;
        float offsetZ = offset.length > 2 ? offset[2] : 0.0f;
        if (offsetX == 0.0f && offsetY == 0.0f && offsetZ == 0.0f) {
            return basePosition;
        }

        var localOffset = new Vector3d(offsetX, offsetY, offsetZ);
        RotationTuple.get(rotationIndex).applyRotationTo(localOffset);
        return new Vector3d(
            basePosition.x + localOffset.x,
            basePosition.y + localOffset.y,
            basePosition.z + localOffset.z
        );
    }
}
