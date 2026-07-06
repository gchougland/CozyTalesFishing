package com.hexvane.cozytalefishing.fish;



import com.hexvane.cozytalefishing.fishing.FishingDebugLog;

import com.hypixel.hytale.component.CommandBuffer;

import com.hypixel.hytale.component.Ref;

import com.hypixel.hytale.server.core.universe.world.World;

import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;

import java.util.List;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import org.joml.Vector3d;



/** Shared fish shadow spawn selection used by natural spawning and debug commands. */

public final class FishShadowSpawner {

    private FishShadowSpawner() {}



    public static boolean trySpawnNearPlayer(

        @Nonnull CommandBuffer<EntityStore> commandBuffer,

        @Nonnull Ref<EntityStore> playerRef,

        @Nonnull World world,

        @Nonnull Vector3d playerPos,

        @Nonnull FishingModConfig config,

        int playerEnvironmentIndex,

        @Nonnull FishShadowSpawnDiagnostics.Report report

    ) {

        List<FishShadowSpawnHelper.WaterColumn> waterColumns = collectNearbyWaterColumns(world, playerPos, config);

        if (waterColumns.isEmpty()) {

            report.skip(FishShadowSpawnDiagnostics.SkipReason.NO_WATER_COLUMNS);

            return false;

        }



        WaterBodyClassifier.Context classifyContext = new WaterBodyClassifier.Context(config.getMaxFloodFillsPerSpawnCheck());

        ThreadLocalRandom random = ThreadLocalRandom.current();



        for (int attempt = 0; attempt < config.getSpawnAttemptsPerCheck(); attempt++) {

            FishShadowSpawnHelper.WaterColumn column = waterColumns.get(random.nextInt(waterColumns.size()));

            int blockX = column.blockX();

            int blockZ = column.blockZ();



            WaterBodyType bodyType =

                WaterBodyClassifier.classify(world, blockX, column.surfaceY(), blockZ, classifyContext);

            if (bodyType == null) {

                bodyType = WaterBodyType.Pond;

            }



            int environmentIndex = resolveEnvironmentIndex(world, column);

            String biomeName = WaterBodyClassifier.getBiomeName(world, blockX, blockZ);

            FishShadowSpawnDiagnostics.SpeciesFilterStats filterStats =

                FishShadowSpawnDiagnostics.analyzeSpeciesFilter(

                    bodyType,

                    environmentIndex,

                    blockX,

                    blockZ,

                    column.surfaceY(),

                    world,

                    playerEnvironmentIndex

                );



            FishSpeciesAsset species =

                pickSpecies(bodyType, blockX, blockZ, column.surfaceY(), environmentIndex, world, playerEnvironmentIndex, config, random);

            if (species == null) {

                report.recordAttemptFailure(FishShadowSpawnDiagnostics.AttemptFailure.NO_SPECIES);

                report.addAttemptDetail(

                    String.format(

                        "attempt %d @ (%d,%d) body=%s env=%d depth=%d biome=%s underground=%s -> NO_SPECIES [%s]",

                        attempt + 1,

                        blockX,

                        blockZ,

                        bodyType,

                        environmentIndex,

                        column.depth(),

                        biomeName != null ? biomeName : "unknown",

                        FishShadowSpawnHelper.isUnderground(

                            world,

                            blockX,

                            blockZ,

                            column.surfaceY(),

                            config.getUndergroundSurfaceOffset()

                        ),

                        filterStats.summarize()

                    )

                );

                continue;

            }



            float[] scaleRange = species.getShadowScaleRange();

            float scale = scaleRange[0] + random.nextFloat() * (scaleRange[1] - scaleRange[0]);

            float yaw = random.nextFloat() * (float) (Math.PI * 2.0);



            Ref<EntityStore> shadowRef =

                FishShadowEntityPool.spawnShadow(

                    commandBuffer,

                    species,

                    bodyType,

                    new Vector3d(blockX + 0.5, column.spawnY(), blockZ + 0.5),

                    yaw,

                    scale

                );

            if (shadowRef != null) {

                report.markSpawned();

                report.addAttemptDetail(

                    String.format(

                        "attempt %d @ (%d,%d) body=%s env=%d depth=%d biome=%s -> SUCCESS species=%s",

                        attempt + 1,

                        blockX,

                        blockZ,

                        bodyType,

                        environmentIndex,

                        column.depth(),

                        biomeName != null ? biomeName : "unknown",

                        species.getId()

                    )

                );

                FishingDebugLog.info(

                    "Spawned fish shadow %s body=%s at (%.1f, %.1f, %.1f)",

                    species.getId(),

                    bodyType,

                    blockX + 0.5,

                    column.spawnY(),

                    blockZ + 0.5

                );

                return true;

            }



            report.recordAttemptFailure(FishShadowSpawnDiagnostics.AttemptFailure.ENTITY_SPAWN_FAILED);

            report.addAttemptDetail(

                String.format(

                    "attempt %d @ (%d,%d) body=%s species=%s -> ENTITY_SPAWN_FAILED (missing model asset?)",

                    attempt + 1,

                    blockX,

                    blockZ,

                    bodyType,

                    species.getId()

                )

            );

        }

        return false;
    }

    @Nonnull
    private static List<FishShadowSpawnHelper.WaterColumn> collectNearbyWaterColumns(

        @Nonnull World world,

        @Nonnull Vector3d playerPos,

        @Nonnull FishingModConfig config

    ) {

        int searchRadius = (int) config.getSpawnRadiusMax();

        int centerX = (int) Math.floor(playerPos.x);

        int centerZ = (int) Math.floor(playerPos.z);

        List<FishShadowSpawnHelper.WaterColumn> columns = new ArrayList<>();

        int minDepth = Math.min(config.getMinWaterDepthBlocks(), 1);



        for (int dx = -searchRadius; dx <= searchRadius; dx++) {

            for (int dz = -searchRadius; dz <= searchRadius; dz++) {

                double distSq = dx * (double) dx + dz * (double) dz;

                if (distSq > searchRadius * searchRadius) {

                    continue;

                }

                int x = centerX + dx;

                int z = centerZ + dz;

                FishShadowSpawnHelper.WaterColumn column = FishShadowSpawnHelper.findWaterColumnAt(world, x, z, minDepth);

                if (column != null) {

                    columns.add(column);

                }

            }

        }

        return columns;

    }



    private static int resolveEnvironmentIndex(@Nonnull World world, @Nonnull FishShadowSpawnHelper.WaterColumn column) {

        long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(column.blockX(), column.blockZ());

        var chunkRef = world.getChunkStore().getChunkReference(chunkIndex);

        if (chunkRef == null) {

            return 0;

        }

        var worldChunk = world.getChunkStore().getStore().getComponent(chunkRef, com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk.getComponentType());

        if (worldChunk == null) {

            return 0;

        }

        return worldChunk.getBlockChunk().getEnvironment(column.blockX(), column.surfaceY(), column.blockZ());

    }



    @Nullable

    private static FishSpeciesAsset pickSpecies(

        @Nonnull WaterBodyType bodyType,

        int blockX,

        int blockZ,

        int surfaceY,

        int environmentIndex,

        @Nonnull World world,

        int playerEnvironmentIndex,

        @Nonnull FishingModConfig config,

        @Nonnull ThreadLocalRandom random

    ) {

        List<FishSpeciesAsset> eligible =

            filterSpecies(bodyType, blockX, blockZ, surfaceY, environmentIndex, world, playerEnvironmentIndex, true);

        if (eligible.isEmpty()) {

            eligible = filterSpecies(bodyType, blockX, blockZ, surfaceY, environmentIndex, world, playerEnvironmentIndex, false);

        }

        if (eligible.isEmpty()) {

            eligible = new ArrayList<>(FishSpeciesRegistry.getSpeciesForWaterBody(bodyType));

        }

        return weightedPick(eligible, config.getGlobalSpawnWeightMultiplier(), random);

    }



    @Nonnull

    private static List<FishSpeciesAsset> filterSpecies(

        @Nonnull WaterBodyType bodyType,

        int blockX,

        int blockZ,

        int surfaceY,

        int environmentIndex,

        @Nonnull World world,

        int playerEnvironmentIndex,

        boolean strictEnvironment

    ) {

        List<FishSpeciesAsset> eligible = new ArrayList<>();

        for (FishSpeciesAsset species : FishSpeciesRegistry.getSpeciesForWaterBody(bodyType)) {

            if (!species.matchesWaterBody(bodyType)) {

                continue;

            }

            if (strictEnvironment && !FishShadowSpawnHelper.matchesSpawnEnvironment(

                species,

                environmentIndex,

                world,

                blockX,

                blockZ,

                playerEnvironmentIndex

            )) {

                continue;

            }

            if (!matchesUnderground(world, blockX, blockZ, surfaceY, species)) {

                continue;

            }

            eligible.add(species);

        }

        return eligible;

    }



    private static boolean matchesUnderground(

        @Nonnull World world,

        int x,

        int z,

        int surfaceY,

        @Nonnull FishSpeciesAsset species

    ) {

        FishingModConfig config = FishingModConfig.get();

        boolean underground =

            FishShadowSpawnHelper.isUnderground(world, x, z, surfaceY, config.getUndergroundSurfaceOffset());

        if (species.isUndergroundOnly()) {

            return underground;

        }

        return !underground;

    }



    @Nullable

    private static FishSpeciesAsset weightedPick(

        @Nonnull List<FishSpeciesAsset> species,

        float globalMultiplier,

        @Nonnull ThreadLocalRandom random

    ) {

        if (species.isEmpty()) {

            return null;

        }

        float total = 0.0f;

        for (FishSpeciesAsset entry : species) {

            total += entry.getEffectiveSpawnWeight(globalMultiplier);

        }

        if (total <= 0.0f) {

            return species.get(random.nextInt(species.size()));

        }

        float roll = random.nextFloat() * total;

        for (FishSpeciesAsset entry : species) {

            roll -= entry.getEffectiveSpawnWeight(globalMultiplier);

            if (roll <= 0.0f) {

                return entry;

            }

        }

        return species.get(species.size() - 1);

    }

}


