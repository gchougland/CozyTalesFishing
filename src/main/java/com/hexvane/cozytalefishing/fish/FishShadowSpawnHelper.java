package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class FishShadowSpawnHelper {
    /** Small offset above the top face of the surface water block. */
    public static final double SURFACE_Y_OFFSET = 0.02;

    private static final Pattern ZONE_PREFIX_PATTERN = Pattern.compile("zone(\\d+)", Pattern.CASE_INSENSITIVE);

    public enum EnvironmentMatchMode {
        /** Require world zone, environment index, and biome rules. */
        STRICT,
        /** Require world zone only (used when no species match strict rules). */
        ZONE_ONLY
    }

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
        return matchesSpawnEnvironment(species, environmentIndex, world, x, z, EnvironmentMatchMode.STRICT);
    }

    public static boolean matchesSpawnEnvironment(
        @Nonnull FishSpeciesAsset species,
        int environmentIndex,
        @Nonnull World world,
        int x,
        int z,
        int playerEnvironmentIndex
    ) {
        return matchesSpawnEnvironment(species, environmentIndex, world, x, z, EnvironmentMatchMode.STRICT);
    }

    public static boolean matchesSpawnEnvironment(
        @Nonnull FishSpeciesAsset species,
        int environmentIndex,
        @Nonnull World world,
        int x,
        int z,
        @Nonnull EnvironmentMatchMode mode
    ) {
        return matchesSpawnEnvironment(species, environmentIndex, world, x, z, mode, null);
    }

    public static boolean matchesSpawnEnvironment(
        @Nonnull FishSpeciesAsset species,
        int environmentIndex,
        @Nonnull World world,
        int x,
        int z,
        @Nonnull EnvironmentMatchMode mode,
        @Nullable FishingSpawnRegionContext regionContext
    ) {
        String worldZone = getWorldZonePrefix(world, x, z);
        if (regionContext != null) {
            if (regionContext.getEffectiveZonePrefix() != null) {
                worldZone = regionContext.getEffectiveZonePrefix();
            }
            if (!regionContext.isIgnoreWorldZoneGate() && worldZone != null && !speciesSupportsWorldZone(species, worldZone)) {
                return false;
            }
        } else if (worldZone != null && !speciesSupportsWorldZone(species, worldZone)) {
            return false;
        }

        String speciesZone = getSpeciesZonePrefix(species);
        if (speciesZone != null && worldZone != null && !zonesMatch(speciesZone, worldZone)) {
            return false;
        }
        if (mode == EnvironmentMatchMode.ZONE_ONLY) {
            return speciesZone != null && worldZone != null && zonesMatch(speciesZone, worldZone);
        }

        if (regionContext != null && regionContext.getRegionEnvironmentIndices().length > 0) {
            int[] regionIndices = regionContext.getRegionEnvironmentIndices();
            int[] speciesIndices = species.getAllowedEnvironmentIndices();
            if (speciesIndices.length > 0) {
                for (int speciesIndex : speciesIndices) {
                    for (int regionIndex : regionIndices) {
                        if (speciesIndex == regionIndex) {
                            return true;
                        }
                    }
                }
            }
        }

        int[] allowed = species.getAllowedEnvironmentIndices();
        if (allowed.length > 0) {
            for (int index : allowed) {
                if (index != environmentIndex) {
                    continue;
                }
                String envZone = FishSpeciesRegistry.getEnvironmentZonePrefix(index);
                if (envZone == null || worldZone == null || zonesMatch(envZone, worldZone)) {
                    return true;
                }
            }
        }

        String biomeName = regionContext != null && regionContext.getEffectiveBiome() != null
            ? regionContext.getEffectiveBiome()
            : WaterBodyClassifier.getBiomeName(world, x, z);
        if (biomeName == null) {
            return allowed.length == 0 && !species.getSpawnLocation().hasEnvironments() && !species.getSpawnLocation().hasBiomes();
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
                if (biomeMatchesEnvironmentId(biomeName, envId, worldZone)) {
                    return true;
                }
            }
        }
        if (location.hasZone()) {
            return biomeMatchesZone(biomeName, location.getZone());
        }
        return allowed.length == 0;
    }

    public static boolean matchesUndergroundFilter(
        @Nonnull World world,
        int x,
        int z,
        int surfaceY,
        @Nonnull FishSpeciesAsset species,
        @Nullable FishingSpawnRegionContext regionContext
    ) {
        FishingModConfig config = FishingModConfig.get();
        boolean underground = isUnderground(world, x, z, surfaceY, config.getUndergroundSurfaceOffset());

        String zonePrefix = regionContext != null && regionContext.getEffectiveZonePrefix() != null
            ? regionContext.getEffectiveZonePrefix()
            : getWorldZonePrefix(world, x, z);
        if (zonePrefix != null && zonePrefix.equalsIgnoreCase("Zone4") && !underground) {
            return false;
        }
        if (FishSpeciesRegistry.requiresUndergroundSpawn(species)) {
            return underground;
        }
        if (species.isUndergroundOnly()) {
            return underground;
        }
        return !underground;
    }

    public static boolean matchesUndergroundFilter(
        @Nonnull World world,
        int x,
        int z,
        int surfaceY,
        @Nonnull FishSpeciesAsset species
    ) {
        return matchesUndergroundFilter(world, x, z, surfaceY, species, null);
    }

    @Nullable
    public static String getWorldZoneName(@Nonnull World world, int blockX, int blockZ) {
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator generator)) {
            return null;
        }
        int seed = (int) world.getWorldConfig().getSeed();
        return generator.getZoneBiomeResultAt(seed, blockX, blockZ).getZoneResult().getZone().name();
    }

    public static boolean isZone4(@Nonnull World world, int blockX, int blockZ) {
        String zone = getWorldZonePrefix(world, blockX, blockZ);
        return zone != null && zone.equalsIgnoreCase("Zone4");
    }

    @Nullable
    public static String getWorldZonePrefix(@Nonnull World world, int blockX, int blockZ) {
        return extractZonePrefix(getWorldZoneName(world, blockX, blockZ));
    }

    @Nullable
    public static String getSpeciesZonePrefix(@Nonnull FishSpeciesAsset species) {
        FishSpawnLocation location = species.getSpawnLocation();
        if (location.hasZone()) {
            return extractZonePrefix(location.getZone());
        }
        return FishSpeciesRegistry.getPrimaryZonePrefix(species);
    }

    @Nullable
    public static String extractZonePrefix(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Matcher matcher = ZONE_PREFIX_PATTERN.matcher(id);
        if (matcher.find()) {
            return "Zone" + matcher.group(1);
        }
        return null;
    }

    public static boolean zonesMatch(@Nonnull String left, @Nonnull String right) {
        return left.equalsIgnoreCase(right);
    }

    /** True when the species lists at least one environment (or Zone field) for this world zone. */
    public static boolean speciesSupportsWorldZone(@Nonnull FishSpeciesAsset species, @Nonnull String worldZone) {
        FishSpawnLocation location = species.getSpawnLocation();
        if (location.hasZone()) {
            String required = extractZonePrefix(location.getZone());
            return required == null || zonesMatch(required, worldZone);
        }
        if (!location.hasEnvironments()) {
            return true;
        }
        boolean foundEnvZone = false;
        for (String envId : location.getEnvironments()) {
            String envZone = extractZonePrefix(envId);
            if (envZone == null) {
                continue;
            }
            foundEnvZone = true;
            if (zonesMatch(envZone, worldZone)) {
                return true;
            }
        }
        return !foundEnvZone;
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

    private static boolean biomeMatchesEnvironmentId(
        @Nonnull String biomeName,
        @Nonnull String envId,
        @Nullable String worldZone
    ) {
        String envZone = extractZonePrefix(envId);
        if (envZone != null && worldZone != null && !zonesMatch(envZone, worldZone)) {
            return false;
        }

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
