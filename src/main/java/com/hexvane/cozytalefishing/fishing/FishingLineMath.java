package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import javax.annotation.Nonnull;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class FishingLineMath {
    private FishingLineMath() {}

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float ropeSegmentLength(float tipToBobberDistance, float maxLength) {
        float ropeLength = Math.min(tipToBobberDistance * FishingConstants.ROPE_SLACK_FACTOR, maxLength);
        return Math.max(ropeLength / FishingConstants.SEGMENT_COUNT, FishingConstants.BASE_SEGMENT_LENGTH * 0.1f);
    }

    /**
     * Orients an entity so local +Y (the segment model length axis) points along {@code direction}.
     * Entity yaw/pitch aim forward (-Z); quads extend along +Y, so use an axis rotation instead.
     */
    public static void rotationFromDirection(@Nonnull Vector3d direction, @Nonnull Rotation3f rotation) {
        double lenSq = direction.lengthSquared();
        if (lenSq < 1.0e-8) {
            rotation.set(0.0f, 0.0f, 0.0f);
            return;
        }
        double invLen = 1.0 / Math.sqrt(lenSq);
        double dx = direction.x * invLen;
        double dy = direction.y * invLen;
        double dz = direction.z * invLen;

        Vector3d euler = new Quaterniond().rotationTo(0.0, 1.0, 0.0, dx, dy, dz).getEulerAnglesYXZ(new Vector3d());
        rotation.setPitch(PhysicsMath.normalizeTurnAngle((float) euler.x));
        rotation.setYaw(PhysicsMath.normalizeTurnAngle((float) euler.y));
        rotation.setRoll(PhysicsMath.normalizeTurnAngle((float) euler.z));
    }

    public static void integrateVerlet(
        @Nonnull Vector3d[] positions,
        @Nonnull Vector3d[] oldPositions,
        int nodeCount,
        float gravity,
        float dt
    ) {
        float dtSq = dt * dt;
        for (int i = 1; i < nodeCount - 1; i++) {
            Vector3d pos = positions[i];
            Vector3d old = oldPositions[i];
            double vx = pos.x - old.x;
            double vy = pos.y - old.y;
            double vz = pos.z - old.z;
            old.set(pos);
            pos.x += vx;
            pos.y += vy - gravity * dtSq;
            pos.z += vz;
        }
    }

    public static void satisfyDistanceConstraints(
        @Nonnull Vector3d[] positions,
        int nodeCount,
        float segmentLength,
        int iterations
    ) {
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < nodeCount - 1; i++) {
                constrainPair(positions[i], positions[i + 1], segmentLength);
            }
        }
    }

    public static void enforceMaxLength(
        @Nonnull Vector3d anchor,
        @Nonnull Vector3d bobber,
        float maxLength
    ) {
        double dx = bobber.x - anchor.x;
        double dy = bobber.y - anchor.y;
        double dz = bobber.z - anchor.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= maxLength * maxLength) {
            return;
        }
        double dist = Math.sqrt(distSq);
        double scale = maxLength / dist;
        bobber.x = anchor.x + dx * scale;
        bobber.y = anchor.y + dy * scale;
        bobber.z = anchor.z + dz * scale;
    }

    private static void constrainPair(@Nonnull Vector3d a, @Nonnull Vector3d b, float restLength) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < 1.0e-10) {
            return;
        }
        double dist = Math.sqrt(distSq);
        double diff = (dist - restLength) / dist;
        double offsetX = dx * diff * 0.5;
        double offsetY = dy * diff * 0.5;
        double offsetZ = dz * diff * 0.5;
        a.x += offsetX;
        a.y += offsetY;
        a.z += offsetZ;
        b.x -= offsetX;
        b.y -= offsetY;
        b.z -= offsetZ;
    }
}
