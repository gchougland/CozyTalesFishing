package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Chat diagnostics for verifying fish spawn rules (when spawn diagnostics are enabled). */
public final class FishSpeciesSpawnDebug {
    private FishSpeciesSpawnDebug() {}

    public static void sendCatchDiagnosticsIfEnabled(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef playerRefComponent,
        @Nonnull FishSpeciesAsset species,
        @Nullable FishShadowComponent shadow,
        @Nullable Vector3d catchPosition
    ) {
        if (!FishingModConfig.get().isEnableSpawnDiagnostics()) {
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        StringBuilder text = new StringBuilder();
        text.append("[CozyTales Fishing] Spawn requirements for ").append(species.getId()).append('\n');
        appendSpawnRequirements(text, species);

        if (world != null && shadow != null && catchPosition != null) {
            text.append('\n').append("Catch context:");
            appendCatchContext(text, commandBuffer, playerRef, world, species, shadow, catchPosition);
        }

        playerRefComponent.sendMessage(Message.raw(text.toString()));
    }

    private static void appendSpawnRequirements(@Nonnull StringBuilder text, @Nonnull FishSpeciesAsset species) {
        text.append("  rarity=").append(species.getRarity().name());
        text.append(", weight=").append(species.getSpawnWeight());
        text.append(", shadow=").append(FishSpeciesMetadataFormatter.formatShadowType(species));
        text.append('\n');
        text.append("  spawnRules:\n");
        FishSpawnRules rules = species.getSpawnRules();
        text.append("    dayTime=").append(FishSpeciesMetadataFormatter.formatDayTimeRule(rules.getDayTime())).append('\n');
        text.append("    weather=").append(FishSpeciesMetadataFormatter.formatWeatherRule(rules.getWeather())).append('\n');
        text.append("    waterBody=").append(FishSpeciesMetadataFormatter.formatWaterBodyTypes(species.getWaterBodyTypes()));
        text.append(" (").append(FishSpeciesMetadataFormatter.formatRuleMode(rules.getWaterBody().getMode())).append(")\n");
        text.append("    underground=").append(FishSpeciesMetadataFormatter.formatUnderground(species.isUndergroundOnly()));
        if (rules.getUnderground().getMode() != null) {
            text.append(" (").append(FishSpeciesMetadataFormatter.formatRuleMode(rules.getUnderground().getMode())).append(')');
        }
        text.append('\n');

        FishSpawnLocation location = species.getSpawnLocation();
        if (location.hasZone()) {
            text.append("  zone=").append(location.getZone());
            text.append('\n');
        }
        if (location.hasEnvironments()) {
            text.append("  environments=").append(Arrays.toString(location.getEnvironments()));
            text.append('\n');
        }
        if (location.hasBiomes()) {
            text.append("  biomes=").append(Arrays.toString(location.getBiomes()));
            text.append('\n');
        }

        int[] envIndices = species.getAllowedEnvironmentIndices();
        if (envIndices.length > 0) {
            text.append("  resolvedEnvIndices=").append(Arrays.toString(envIndices));
            text.append('\n');
        }
    }

    private static void appendCatchContext(
        @Nonnull StringBuilder text,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull World world,
        @Nonnull FishSpeciesAsset species,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Vector3d catchPosition
    ) {
        int blockX = (int) Math.floor(catchPosition.x);
        int blockZ = (int) Math.floor(catchPosition.z);
        int surfaceY = FishShadowSpawnHelper.findSurfaceWaterBlockY(world, blockX, blockZ);
        FishingSpawnRegionContext regionContext =
            surfaceY >= 0
                ? FishingSpawnRegionRegistry.resolve(world.getWorldConfig().getUuid(), blockX, surfaceY, blockZ)
                : null;
        String biome = WaterBodyClassifier.getBiomeName(world, blockX, blockZ);
        if (regionContext != null && regionContext.getEffectiveBiome() != null) {
            biome = regionContext.getEffectiveBiome();
        }
        int catchEnvironmentIndex = surfaceY >= 0 ? resolveEnvironmentIndex(world, blockX, surfaceY, blockZ) : -1;
        int playerEnvironmentIndex = resolvePlayerEnvironmentIndex(commandBuffer, playerRef);
        boolean underground =
            surfaceY >= 0
                && FishShadowSpawnHelper.isUnderground(
                    world,
                    blockX,
                    blockZ,
                    surfaceY,
                    FishingModConfig.get().getUndergroundSurfaceOffset()
                );

        text.append('\n');
        text.append("  pos=(")
            .append(String.format(Locale.US, "%.1f", catchPosition.x))
            .append(", ")
            .append(String.format(Locale.US, "%.1f", catchPosition.z))
            .append("), biome=")
            .append(biome != null ? biome : "unknown");
        String worldZone = FishShadowSpawnHelper.getWorldZoneName(world, blockX, blockZ);
        if (regionContext != null && regionContext.getEffectiveZonePrefix() != null) {
            text.append(", effectiveZone=").append(regionContext.getEffectiveZonePrefix());
        } else if (worldZone != null) {
            text.append(", worldZone=").append(worldZone);
        }
        if (regionContext != null) {
            text.append(", spawnRegion=").append(regionContext.getRegion().getId());
        }
        text.append('\n');
        text.append("  shadowWaterBody=").append(shadow.getWaterBodyType().name());
        if (regionContext != null && regionContext.getWaterBodyOverride() != null) {
            text.append(" (region override=").append(regionContext.getWaterBodyOverride().name()).append(')');
        }
        text.append(", catchEnvIndex=").append(catchEnvironmentIndex);
        text.append(", playerEnvIndex=").append(playerEnvironmentIndex);
        text.append(", underground=").append(underground);
        text.append('\n');

        if (surfaceY >= 0) {
            FishingModConfig config = FishingModConfig.get();
            FishShadowSpawnHelper.SpawnConditions spawnConditions =
                FishShadowSpawnHelper.resolveSpawnConditions(world, catchEnvironmentIndex);
            FishSpawnRulesEvaluator.SpawnEvaluationContext context =
                new FishSpawnRulesEvaluator.SpawnEvaluationContext(
                    shadow.getWaterBodyType(),
                    catchEnvironmentIndex,
                    blockX,
                    blockZ,
                    surfaceY,
                    world,
                    spawnConditions,
                    regionContext,
                    config
                );
            FishSpawnRulesEvaluator.SpawnRuleResult result =
                FishSpawnRulesEvaluator.evaluate(species, context);
            text.append("  spawnRulesEligible=").append(result.eligible());
            text.append(", weightMultiplier=").append(String.format(Locale.US, "%.2f", result.weightMultiplier()));
            text.append('\n');
            text.append("  ruleMatches=").append(result.ruleMatches());
            text.append('\n');
            WorldTimeResource worldTime = spawnConditions.worldTime();
            text.append("  currentHour=")
                .append(worldTime.getCurrentHour())
                .append(", weatherIndex=")
                .append(spawnConditions.weatherIndex());
            text.append('\n');
        }
    }

    private static int resolvePlayerEnvironmentIndex(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        WeatherTracker weatherTracker = commandBuffer.getComponent(playerRef, WeatherTracker.getComponentType());
        return weatherTracker != null ? weatherTracker.getEnvironmentId() : -1;
    }

    private static int resolveEnvironmentIndex(@Nonnull World world, int blockX, int blockY, int blockZ) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        var chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
        if (chunkRef == null) {
            return -1;
        }
        WorldChunk worldChunk = world.getChunkStore().getStore().getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
            return -1;
        }
        return worldChunk.getBlockChunk().getEnvironment(blockX, blockY, blockZ);
    }
}
