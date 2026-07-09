package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.FishShadowSpawnHelper;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

final class FishingLineGroundUtil {
    private FishingLineGroundUtil() {}

    static void clampAboveGround(@Nonnull World world, @Nonnull Vector3d[] nodes, int nodeCount) {
        for (int i = 1; i < nodeCount - 1; i++) {
            Vector3d node = nodes[i];
            int blockX = (int) Math.floor(node.x);
            int blockZ = (int) Math.floor(node.z);
            int floorBlockY = FishShadowSpawnHelper.findSolidFloorBlockYBelow(world, blockX, blockZ, (int) Math.floor(node.y));
            if (floorBlockY < 0) {
                continue;
            }
            double floorY = floorBlockY + 1.0 + FishingConstants.GROUND_CLEARANCE;
            if (node.y < floorY) {
                node.y = floorY;
            }
        }
    }
}
