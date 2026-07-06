package com.hexvane.cozytalefishing.fish;

import org.joml.Vector3d;
import javax.annotation.Nonnull;

public final class FishShadowVision {
    private FishShadowVision() {}

    public static boolean isInHorizontalCone(
        @Nonnull Vector3d observerPos,
        float observerYawRadians,
        @Nonnull Vector3d targetPos,
        float range,
        float halfAngleDegrees
    ) {
        double dx = targetPos.x - observerPos.x;
        double dz = targetPos.z - observerPos.z;
        double distSq = dx * dx + dz * dz;
        if (distSq > range * range || distSq < 1.0e-6) {
            return distSq < 1.0e-6;
        }
        double dist = Math.sqrt(distSq);
        double forwardX = -Math.sin(observerYawRadians);
        double forwardZ = Math.cos(observerYawRadians);
        double dot = (dx * forwardX + dz * forwardZ) / dist;
        double halfAngleRad = Math.toRadians(halfAngleDegrees);
        return dot >= Math.cos(halfAngleRad);
    }

    public static float yawToward(@Nonnull Vector3d from, @Nonnull Vector3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return yawFromDirection((float) dx, (float) dz);
    }

    public static float yawFromDirection(float dirX, float dirZ) {
        return (float) Math.atan2(-dirX, dirZ);
    }

    /** Yaw matching wander movement where velocity uses sin/cos of the rotation angle. */
    public static float swimYaw(float dirX, float dirZ) {
        return (float) Math.atan2(dirX, dirZ);
    }

    /** @deprecated Use {@link #swimYaw(float, float)} — the +PI flip was incorrect for this model. */
    @Deprecated
    public static float shadowMovementYaw(float dirX, float dirZ) {
        return swimYaw(dirX, dirZ);
    }
}
