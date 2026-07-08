package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingDebugLog;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WaterBodyClassifier {
    private static final ThreadLocal<BfsState> BFS_STATE = ThreadLocal.withInitial(BfsState::new);
    private static final ConcurrentHashMap<UUID, WorldCache> WORLD_CACHES = new ConcurrentHashMap<>();

    private static int tier1Hits;
    private static int tier2Hits;
    private static int cacheHits;

    private WaterBodyClassifier() {}

    public static final class Context {
        private int floodFillBudget;

        public Context(int floodFillBudget) {
            this.floodFillBudget = floodFillBudget;
        }

        public boolean consumeFloodFillBudget() {
            if (floodFillBudget <= 0) {
                return false;
            }
            floodFillBudget--;
            return true;
        }
    }

    @Nullable
    public static WaterBodyType classify(
        @Nonnull World world,
        int blockX,
        int surfaceY,
        int blockZ,
        @Nonnull Context context
    ) {
        return classify(world, blockX, surfaceY, blockZ, context, -1);
    }

    @Nullable
    public static WaterBodyType classify(
        @Nonnull World world,
        int blockX,
        int surfaceY,
        int blockZ,
        @Nonnull Context context,
        int environmentIndex
    ) {
        FishingModConfig config = FishingModConfig.get();
        WorldCache cache = WORLD_CACHES.computeIfAbsent(world.getWorldConfig().getUuid(), ignored -> new WorldCache(config.getWaterBodyCacheSize()));
        long regionKey = regionKey(blockX, surfaceY, blockZ, config.getWaterBodyCellSize());
        WaterBodyType cached = cache.get(regionKey);
        if (cached != null) {
            cacheHits++;
            return cached;
        }

        WorldChunk worldChunk = getWorldChunk(world, blockX, blockZ);
        if (worldChunk == null) {
            return null;
        }

        String biomeName = resolveBiomeName(world, blockX, blockZ);
        WaterBodyType tier1 = resolveTier1Biome(biomeName);
        if (tier1 != null) {
            tier1Hits++;
            cache.put(regionKey, tier1);
            return tier1;
        }

        WaterBodyType classified;
        if (!context.consumeFloodFillBudget()) {
            classified = classifyFromLocalColumn(world, blockX, surfaceY, blockZ, config, biomeName, environmentIndex);
        } else {
            classified = classifyByGeometry(world, blockX, surfaceY, blockZ, config, biomeName, environmentIndex);
            tier2Hits++;
        }

        if (classified != null) {
            cache.put(regionKey, classified);
        }
        return classified;
    }

    /**
     * Corrects geometry-based misclassification when the column is in an ocean/shore biome or environment.
     * Freshwater rivers named in the biome are preserved.
     */
    @Nonnull
    public static WaterBodyType correctForOceanContext(
        @Nonnull WaterBodyType classified,
        @Nullable String biomeName,
        int environmentIndex
    ) {
        if (classified == WaterBodyType.Ocean) {
            return WaterBodyType.Ocean;
        }
        if (classified == WaterBodyType.River && isRiverBiomeName(biomeName)) {
            return WaterBodyType.River;
        }
        if (isOceanBiomeName(biomeName) || isOceanEnvironment(environmentIndex)) {
            return WaterBodyType.Ocean;
        }
        return classified;
    }

    @Nullable
    private static WaterBodyType resolveTier1Biome(@Nullable String biomeName) {
        if (biomeName == null) {
            return null;
        }
        WaterBodyType override = WaterBodyBiomeOverridesAsset.getOrEmpty().resolveBiomeName(biomeName);
        if (override != null) {
            return override;
        }
        String lower = biomeName.toLowerCase();
        if (isRiverBiomeName(biomeName)) {
            return WaterBodyType.River;
        }
        if (isOceanBiomeName(biomeName)) {
            return WaterBodyType.Ocean;
        }
        if (lower.contains("pond") || lower.contains("oasis") || lower.contains("lake")) {
            return WaterBodyType.Pond;
        }
        return null;
    }

    private static boolean isRiverBiomeName(@Nullable String biomeName) {
        if (biomeName == null) {
            return false;
        }
        String lower = biomeName.toLowerCase();
        return lower.contains("river") || lower.contains("creek") || lower.contains("stream");
    }

    private static boolean isOceanBiomeName(@Nullable String biomeName) {
        if (biomeName == null) {
            return false;
        }
        String lower = biomeName.toLowerCase();
        return lower.contains("shore")
            || lower.contains("ocean")
            || lower.contains("shallow_ocean")
            || lower.contains("coast")
            || lower.contains("beach")
            || lower.contains("reef")
            || lower.contains("sea");
    }

    private static boolean isOceanEnvironment(int environmentIndex) {
        return environmentIndex >= 0 && FishSpeciesRegistry.getOceanEnvironmentIndices().contains(environmentIndex);
    }

    public static boolean isOceanEnvironmentForSpawn(int environmentIndex) {
        return isOceanEnvironment(environmentIndex);
    }

    public static void logStatsAndReset() {
        if (tier1Hits + tier2Hits + cacheHits == 0) {
            return;
        }
        FishingDebugLog.info(
            "WaterBodyClassifier stats: cacheHits=%d tier1=%d tier2=%d",
            cacheHits,
            tier1Hits,
            tier2Hits
        );
        tier1Hits = 0;
        tier2Hits = 0;
        cacheHits = 0;
    }

    public static void invalidateCaches() {
        WORLD_CACHES.clear();
    }

    @Nullable
    private static WaterBodyType classifyFromLocalColumn(
        @Nonnull World world,
        int blockX,
        int surfaceY,
        int blockZ,
        @Nonnull FishingModConfig config,
        @Nullable String biomeName,
        int environmentIndex
    ) {
        int depth = measureDepth(world, blockX, surfaceY, blockZ);
        if (depth <= 0) {
            return null;
        }
        if (isOceanBiomeName(biomeName) || isOceanEnvironment(environmentIndex)) {
            return WaterBodyType.Ocean;
        }
        if (depth < config.getOceanMinDepth() && depth <= 4) {
            return WaterBodyType.Pond;
        }
        WaterBodyType byDepth = depth >= config.getOceanMinDepth() ? WaterBodyType.Ocean : WaterBodyType.River;
        return correctForOceanContext(byDepth, biomeName, environmentIndex);
    }

    @Nullable
    private static WaterBodyType classifyByGeometry(
        @Nonnull World world,
        int startX,
        int surfaceY,
        int startZ,
        @Nonnull FishingModConfig config,
        @Nullable String biomeName,
        int environmentIndex
    ) {
        BfsState state = BFS_STATE.get();
        state.reset(config.getFloodFillMaxRadius());

        int radius = config.getFloodFillMaxRadius();
        int originX = startX - radius;
        int originZ = startZ - radius;
        int size = radius * 2 + 1;

        state.enqueue(startX, startZ);
        state.markVisited(startX, startZ, originX, originZ, size);

        int blockCount = 0;
        int minX = startX;
        int maxX = startX;
        int minZ = startZ;
        int maxZ = startZ;
        int maxDepth = 0;

        while (!state.queueEmpty()) {
            int x = state.dequeueX();
            int z = state.dequeueZ();
            blockCount++;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            maxDepth = Math.max(maxDepth, measureDepth(world, x, surfaceY, z));

            if (blockCount >= config.getFloodFillMaxBlocks()) {
                break;
            }

            expand(world, surfaceY, x + 1, z, originX, originZ, size, state);
            expand(world, surfaceY, x - 1, z, originX, originZ, size, state);
            expand(world, surfaceY, x, z + 1, originX, originZ, size, state);
            expand(world, surfaceY, x, z - 1, originX, originZ, size, state);
        }

        int width = maxX - minX + 1;
        int length = maxZ - minZ + 1;
        int maxDim = Math.max(width, length);
        int minDim = Math.max(1, Math.min(width, length));
        float aspectRatio = maxDim / (float) minDim;

        WaterBodyType geometry;
        if (blockCount <= config.getPondMaxBlocks() && maxDim <= config.getPondMaxDimension()) {
            geometry = WaterBodyType.Pond;
        } else if (aspectRatio >= config.getRiverMinAspectRatio()) {
            geometry = WaterBodyType.River;
        } else {
            boolean deepEnough = maxDepth >= config.getOceanMinDepth();
            boolean wideEnough = blockCount >= config.getOceanMinBlocks();
            if (deepEnough && wideEnough && aspectRatio < 2.5f) {
                geometry = WaterBodyType.Ocean;
            } else {
                geometry = aspectRatio > 2.0f ? WaterBodyType.River : WaterBodyType.Pond;
            }
        }
        return correctForOceanContext(geometry, biomeName, environmentIndex);
    }

    private static void expand(
        @Nonnull World world,
        int surfaceY,
        int x,
        int z,
        int originX,
        int originZ,
        int size,
        @Nonnull BfsState state
    ) {
        if (Math.abs(x - originX - state.centerOffset) > state.radius || Math.abs(z - originZ - state.centerOffset) > state.radius) {
            return;
        }
        if (state.isVisited(x, z, originX, originZ, size)) {
            return;
        }
        if (!isWaterAt(world, x, surfaceY, z)) {
            return;
        }
        state.markVisited(x, z, originX, originZ, size);
        state.enqueue(x, z);
    }

    private static int measureDepth(@Nonnull World world, int x, int surfaceY, int z) {
        int depth = 0;
        for (int y = surfaceY; y >= surfaceY - 12; y--) {
            if (isWaterAt(world, x, y, z)) {
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    private static boolean isWaterAt(@Nonnull World world, int x, int y, int z) {
        ChunkStore chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null) {
            return false;
        }
        FluidSection fluidSection = chunkStore.getStore().getComponentConcurrent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return false;
        }
        return fluidSection.getFluidId(x, y, z) != Fluid.EMPTY_ID;
    }

    @Nullable
    private static WorldChunk getWorldChunk(@Nonnull World world, int blockX, int blockZ) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        var chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
        if (chunkRef == null) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(chunkRef, WorldChunk.getComponentType());
    }

    @Nullable
    public static String getBiomeName(@Nonnull World world, int blockX, int blockZ) {
        return resolveBiomeName(world, blockX, blockZ);
    }

    @Nullable
    private static String resolveBiomeName(@Nonnull World world, int blockX, int blockZ) {
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator generator)) {
            return null;
        }
        int seed = (int) world.getWorldConfig().getSeed();
        return generator.getZoneBiomeResultAt(seed, blockX, blockZ).getBiome().getName();
    }

    private static long regionKey(int x, int y, int z, int cellSize) {
        int cell = Math.max(4, cellSize);
        int cx = Math.floorDiv(x, cell);
        int cy = Math.floorDiv(y, cell);
        int cz = Math.floorDiv(z, cell);
        return ((long) cx << 42) ^ ((long) cy << 21) ^ cz;
    }

    private static final class WorldCache {
        private final LinkedHashMap<Long, WaterBodyType> map;

        WorldCache(int maxSize) {
            this.map =
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, WaterBodyType> eldest) {
                        return size() > maxSize;
                    }
                };
        }

        @Nullable
        synchronized WaterBodyType get(long key) {
            return map.get(key);
        }

        synchronized void put(long key, @Nonnull WaterBodyType type) {
            map.put(key, type);
        }
    }

    private static final class BfsState {
        private final int[] queueX = new int[4096];
        private final int[] queueZ = new int[4096];
        private int queueHead;
        private int queueTail;
        private java.util.BitSet visited;
        private int radius;
        private int centerOffset;

        void reset(int radius) {
            this.radius = radius;
            this.centerOffset = radius;
            queueHead = 0;
            queueTail = 0;
            int size = radius * 2 + 1;
            visited = new java.util.BitSet(size * size);
        }

        void enqueue(int x, int z) {
            if (queueTail >= queueX.length) {
                return;
            }
            queueX[queueTail] = x;
            queueZ[queueTail] = z;
            queueTail++;
        }

        boolean queueEmpty() {
            return queueHead >= queueTail;
        }

        int dequeueX() {
            return queueX[queueHead];
        }

        int dequeueZ() {
            return queueZ[queueHead++];
        }

        boolean isVisited(int x, int z, int originX, int originZ, int size) {
            int localX = x - originX;
            int localZ = z - originZ;
            if (localX < 0 || localZ < 0 || localX >= size || localZ >= size) {
                return true;
            }
            return visited.get(localX * size + localZ);
        }

        void markVisited(int x, int z, int originX, int originZ, int size) {
            int localX = x - originX;
            int localZ = z - originZ;
            if (localX < 0 || localZ < 0 || localX >= size || localZ >= size) {
                return;
            }
            visited.set(localX * size + localZ);
        }
    }
}
