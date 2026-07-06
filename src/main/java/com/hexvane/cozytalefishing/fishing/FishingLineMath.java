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
        return ropeSegmentLength(tipToBobberDistance, maxLength, FishingConstants.ROPE_SLACK_FACTOR);
    }

    public static float ropeSegmentLength(float tipToBobberDistance, float maxLength, float slackFactor) {
        float ropeLength = Math.min(tipToBobberDistance * slackFactor, maxLength);
        return Math.max(ropeLength / FishingConstants.SEGMENT_COUNT, FishingConstants.BASE_SEGMENT_LENGTH * 0.1f);
    }

    /** How many fixed-length segment props should be visible along the line. */
    public static int visibleSegmentCount(float arcLengthBlocks) {
        if (arcLengthBlocks < 1.0e-4f) {
            return 0;
        }
        int densityCount =
            (int) Math.ceil(arcLengthBlocks / FishingConstants.BASE_SEGMENT_LENGTH * FishingConstants.SEGMENT_VISUAL_DENSITY);
        // At least one prop per BASE_SEGMENT_LENGTH so placement intervals never exceed prop length (gaps).
        int minCoverage = Math.max(1, (int) Math.ceil(arcLengthBlocks / FishingConstants.BASE_SEGMENT_LENGTH));
        return Math.min(FishingConstants.SEGMENT_COUNT, Math.max(densityCount, minCoverage));
    }

    /** Total length along the rope polyline from rod tip through each node. */
    public static float ropePolylineLength(@Nonnull Vector3d[] nodes, @Nonnull Vector3d tip, int nodeCount) {
        float total = 0.0f;
        Vector3d prev = tip;
        for (int i = 1; i < nodeCount; i++) {
            total += prev.distance(nodes[i]);
            prev = nodes[i];
        }
        return total;
    }

    /** Samples a point at normalized arc length {@code t} in [0, 1] along the rope polyline. */
    public static void sampleRopeAt(
        @Nonnull Vector3d[] nodes,
        @Nonnull Vector3d tip,
        int nodeCount,
        float t,
        float totalLength,
        @Nonnull Vector3d out
    ) {
        t = clamp(t, 0.0f, 1.0f);
        if (totalLength < 1.0e-6f) {
            out.set(tip);
            return;
        }
        float target = totalLength * t;
        float walked = 0.0f;
        Vector3d prev = tip;
        out.set(tip);
        for (int i = 1; i < nodeCount; i++) {
            Vector3d curr = nodes[i];
            float seg = (float) prev.distance(curr);
            if (seg > 1.0e-8f && walked + seg >= target) {
                float local = (target - walked) / seg;
                out.x = prev.x + (curr.x - prev.x) * local;
                out.y = prev.y + (curr.y - prev.y) * local;
                out.z = prev.z + (curr.z - prev.z) * local;
                return;
            }
            walked += seg;
            prev = curr;
        }
        out.set(nodes[nodeCount - 1]);
    }

    /** Evenly distributes rope nodes on a straight line between tip and bobber. */
    public static void layoutNodesOnLine(
        @Nonnull Vector3d[] nodes,
        @Nonnull Vector3d tip,
        @Nonnull Vector3d bobber,
        int nodeCount
    ) {
        if (nodeCount <= 0) {
            return;
        }
        nodes[0].set(tip);
        if (nodeCount == 1) {
            return;
        }
        nodes[nodeCount - 1].set(bobber);
        for (int i = 1; i < nodeCount - 1; i++) {
            double t = i / (double) (nodeCount - 1);
            Vector3d node = nodes[i];
            node.x = tip.x + (bobber.x - tip.x) * t;
            node.y = tip.y + (bobber.y - tip.y) * t;
            node.z = tip.z + (bobber.z - tip.z) * t;
        }
    }

    /** Maps a visible segment index to the rope node index it starts at (evenly spaced along the line). */
    public static int segmentStartNode(int visibleSegmentIndex, int visibleSegmentCount) {
        if (visibleSegmentCount <= 0) {
            return 0;
        }
        return (visibleSegmentIndex * (FishingConstants.NODE_COUNT - 1)) / visibleSegmentCount;
    }

    /** Maps a visible segment index to the rope node index it ends at. */
    public static int segmentEndNode(int visibleSegmentIndex, int visibleSegmentCount) {
        if (visibleSegmentCount <= 0) {
            return 1;
        }
        int end = ((visibleSegmentIndex + 1) * (FishingConstants.NODE_COUNT - 1)) / visibleSegmentCount;
        return Math.min(FishingConstants.NODE_COUNT - 1, Math.max(end, segmentStartNode(visibleSegmentIndex, visibleSegmentCount) + 1));
    }

    /** Pulls intermediate rope nodes toward a straight line while reeling. */
    public static void straightenIntermediateNodes(
        @Nonnull Vector3d[] nodes,
        @Nonnull Vector3d tip,
        @Nonnull Vector3d bobber,
        int nodeCount,
        float strength
    ) {
        if (nodeCount <= 2 || strength <= 0.0f) {
            return;
        }
        for (int i = 1; i < nodeCount - 1; i++) {
            double t = i / (double) (nodeCount - 1);
            double targetX = tip.x + (bobber.x - tip.x) * t;
            double targetY = tip.y + (bobber.y - tip.y) * t;
            double targetZ = tip.z + (bobber.z - tip.z) * t;
            Vector3d node = nodes[i];
            node.x += (targetX - node.x) * strength;
            node.y += (targetY - node.y) * strength;
            node.z += (targetZ - node.z) * strength;
        }
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
