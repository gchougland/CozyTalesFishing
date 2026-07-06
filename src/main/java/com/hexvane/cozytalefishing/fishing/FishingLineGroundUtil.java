package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

final class FishingLineGroundUtil {
    private FishingLineGroundUtil() {}

    static void clampAboveGround(@Nonnull World world, @Nonnull Vector3d[] nodes, int nodeCount) {
        for (int i = 1; i < nodeCount - 1; i++) {
            Vector3d node = nodes[i];
            double floorY = surfaceY(world, node.x, node.z);
            if (floorY > Double.NEGATIVE_INFINITY && node.y < floorY) {
                node.y = floorY;
            }
        }
    }

    private static double surfaceY(@Nonnull World world, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(blockX, blockZ));
        if (chunk == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return chunk.getHeight(blockX, blockZ) + FishingConstants.GROUND_CLEARANCE;
    }
}
