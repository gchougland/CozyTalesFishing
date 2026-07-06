package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class FishShadowSpawnHelper {
    /** Small offset above the top face of the surface water block. */
    public static final double SURFACE_Y_OFFSET = 0.02;

    private FishShadowSpawnHelper() {}

    public static double waterSurfaceWorldY(int waterBlockY) {
        return waterBlockY + 1.0 + SURFACE_Y_OFFSET;
    }

    public static int findSurfaceWaterBlockY(@Nonnull World world, int x, int z) {
        for (int y = 320; y >= 0; y--) {
            if (isWaterAt(world, x, y, z)) {
                return y;
            }
        }
        return -1;
    }

    public static void snapShadowToWaterSurface(@Nullable World world, @Nonnull Vector3d position) {
        if (world == null) {
            return;
        }
        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);
        int surfaceY = findSurfaceWaterBlockY(world, x, z);
        if (surfaceY >= 0) {
            position.y = waterSurfaceWorldY(surfaceY);
        }
    }

    @Nullable
    public static SpawnResult spawnRandomNearPlayer(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull World world
    ) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }

        Vector3d playerPos = transform.getPosition();
        List<FishSpeciesAsset> allSpecies = FishSpeciesRegistry.getAllSpecies();
        if (allSpecies.isEmpty()) {
            return null;
        }

        FishSpeciesAsset species = allSpecies.get((int) (Math.random() * allSpecies.size()));
        WaterColumn column = findNearestWaterColumn(world, playerPos, 16);
        if (column == null) {
            return null;
        }

        int blockX = column.blockX;
        int blockZ = column.blockZ;
        WaterBodyType bodyType =
            WaterBodyClassifier.classify(
                world,
                blockX,
                column.surfaceY,
                blockZ,
                new WaterBodyClassifier.Context(2)
            );
        if (bodyType == null) {
            bodyType = WaterBodyType.Pond;
        }

        float[] scaleRange = species.getShadowScaleRange();
        float scale = scaleRange[0] + (float) Math.random() * (scaleRange[1] - scaleRange[0]);
        float yaw = (float) (Math.random() * Math.PI * 2.0);

        return FishShadowEntityPool.spawnShadow(
            store,
            species,
            bodyType,
            new Vector3d(blockX + 0.5, column.spawnY, blockZ + 0.5),
            yaw,
            scale
        ) != null ? new SpawnResult(species) : null;
    }

    public record SpawnResult(@Nonnull FishSpeciesAsset species) {}

    @Nullable
    private static WaterColumn findNearestWaterColumn(@Nonnull World world, @Nonnull Vector3d origin, int searchRadius) {
        int centerX = (int) Math.floor(origin.x);
        int centerZ = (int) Math.floor(origin.z);
        WaterColumn best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                WaterColumn column = findWaterColumnAt(world, x, z, 1);
                if (column == null) {
                    continue;
                }
                double distSq = dx * (double) dx + dz * (double) dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = column;
                }
            }
        }
        return best;
    }

    @Nullable
    static WaterColumn findWaterColumnAt(@Nonnull World world, int x, int z, int minDepth) {
        int surfaceY = -1;
        for (int y = 320; y >= 0; y--) {
            if (isWaterAt(world, x, y, z)) {
                surfaceY = y;
                break;
            }
        }
        if (surfaceY < 0) {
            return null;
        }
        int bottomY = surfaceY;
        for (int y = surfaceY; y >= 0; y--) {
            if (isWaterAt(world, x, y, z)) {
                bottomY = y;
            } else {
                break;
            }
        }
        int depth = surfaceY - bottomY + 1;
        if (depth < minDepth) {
            return null;
        }
        float spawnY = (float) waterSurfaceWorldY(surfaceY);
        return new WaterColumn(x, z, surfaceY, spawnY, depth);
    }

    static boolean isUnderground(@Nonnull World world, int x, int z, int waterSurfaceBlockY, int coverScanDepth) {
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return false;
        }
        int scanTop = Math.min(waterSurfaceBlockY + coverScanDepth, 320);
        for (int y = waterSurfaceBlockY + 1; y <= scanTop; y++) {
            int blockId = chunk.getBlock(x, y, z);
            if (blockId != BlockType.EMPTY_ID && !isWaterAt(world, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesSpawnEnvironment(
        @Nonnull FishSpeciesAsset species,
        int environmentIndex,
        @Nonnull World world,
        int x,
        int z
    ) {
        return matchesSpawnEnvironment(species, environmentIndex, world, x, z, -1);
    }

    public static boolean matchesSpawnEnvironment(
        @Nonnull FishSpeciesAsset species,
        int environmentIndex,
        @Nonnull World world,
        int x,
        int z,
        int playerEnvironmentIndex
    ) {
        int[] allowed = species.getAllowedEnvironmentIndices();
        if (allowed.length > 0) {
            for (int index : allowed) {
                if (index == environmentIndex || (playerEnvironmentIndex >= 0 && index == playerEnvironmentIndex)) {
                    return true;
                }
            }
        } else {
            return true;
        }

        String biomeName = WaterBodyClassifier.getBiomeName(world, x, z);
        if (biomeName == null) {
            return false;
        }

        FishSpawnLocation location = species.getSpawnLocation();
        if (location.hasBiomes()) {
            for (String biomeId : location.getBiomes()) {
                if (biomeName.equalsIgnoreCase(biomeId) || biomeName.toLowerCase().contains(biomeId.toLowerCase())) {
                    return true;
                }
            }
        }
        if (location.hasEnvironments()) {
            for (String envId : location.getEnvironments()) {
                if (biomeMatchesEnvironmentId(biomeName, envId)) {
                    return true;
                }
            }
        }
        if (location.hasZone()) {
            return biomeMatchesZone(biomeName, location.getZone());
        }
        return false;
    }

    public static boolean hasWaterAt(@Nonnull World world, int blockX, int blockZ) {
        return findSurfaceWaterBlockY(world, blockX, blockZ) >= 0;
    }

    /** Moves the shadow on the XZ plane only if the destination block column contains water. */
    public static boolean tryMoveOnWaterSurface(
        @Nullable World world,
        @Nonnull Vector3d position,
        double deltaX,
        double deltaZ
    ) {
        if (world == null) {
            return false;
        }
        double nextX = position.x + deltaX;
        double nextZ = position.z + deltaZ;
        int blockX = (int) Math.floor(nextX);
        int blockZ = (int) Math.floor(nextZ);
        int surfaceY = findSurfaceWaterBlockY(world, blockX, blockZ);
        if (surfaceY < 0) {
            return false;
        }
        position.x = nextX;
        position.z = nextZ;
        position.y = waterSurfaceWorldY(surfaceY);
        return true;
    }

    @Nullable
    public static Vector3d clampPositionToWater(@Nullable World world, @Nonnull Vector3d position) {
        if (world == null) {
            return null;
        }
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int surfaceY = findSurfaceWaterBlockY(world, blockX, blockZ);
        if (surfaceY < 0) {
            return null;
        }
        return new Vector3d(blockX + 0.5, waterSurfaceWorldY(surfaceY), blockZ + 0.5);
    }

    @Nullable
    static WaterColumn findNearestWaterColumnInRadius(
        @Nonnull World world,
        @Nonnull Vector3d origin,
        int searchRadius,
        int minDepth
    ) {
        int centerX = (int) Math.floor(origin.x);
        int centerZ = (int) Math.floor(origin.z);
        WaterColumn best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                WaterColumn column = findWaterColumnAt(world, x, z, minDepth);
                if (column == null) {
                    continue;
                }
                double distSq = dx * (double) dx + dz * (double) dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = column;
                }
            }
        }
        return best;
    }

    private static boolean biomeMatchesEnvironmentId(@Nonnull String biomeName, @Nonnull String envId) {
        String biome = biomeName.toLowerCase();
        String env = envId.toLowerCase();
        if (env.startsWith("env_")) {
            env = env.substring(4);
        }
        int underscore = env.indexOf('_');
        if (underscore >= 0 && underscore < env.length() - 1) {
            String habitat = env.substring(underscore + 1);
            if (biome.contains(habitat)) {
                return true;
            }
        }
        return biome.contains(env);
    }

    private static boolean biomeMatchesZone(@Nonnull String biomeName, @Nonnull String zone) {
        String biome = biomeName.toLowerCase();
        String zoneLower = zone.toLowerCase();
        return biome.contains(zoneLower);
    }

    private static boolean isWaterAt(@Nonnull World world, int x, int y, int z) {
        var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return false;
        }
        FluidSection fluidSection = world.getChunkStore().getStore().getComponent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return false;
        }
        return fluidSection.getFluidId(x, y, z) != Fluid.EMPTY_ID;
    }

    public record WaterColumn(int blockX, int blockZ, int surfaceY, float spawnY, int depth) {}

    public record WaterScanResult(int shallowColumns, int validColumns) {}

    @Nonnull
    public static WaterScanResult scanWaterColumnsInRadius(
        @Nonnull World world,
        @Nonnull Vector3d origin,
        int searchRadius,
        int minDepth
    ) {
        int centerX = (int) Math.floor(origin.x);
        int centerZ = (int) Math.floor(origin.z);
        int shallow = 0;
        int valid = 0;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                if (dx * (double) dx + dz * (double) dz > searchRadius * searchRadius) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                if (findWaterColumnAt(world, x, z, 1) != null) {
                    shallow++;
                }
                if (findWaterColumnAt(world, x, z, minDepth) != null) {
                    valid++;
                }
            }
        }
        return new WaterScanResult(shallow, valid);
    }
}
