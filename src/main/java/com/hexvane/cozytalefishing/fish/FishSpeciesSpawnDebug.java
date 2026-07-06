package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
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
        text.append(", shadow=").append(species.getShadowType().name());
        text.append('\n');
        text.append("  waterBodyTypes=").append(formatWaterBodyTypes(species.getWaterBodyTypes()));
        text.append('\n');
        text.append("  undergroundOnly=").append(species.isUndergroundOnly());
        text.append('\n');

        float[] dayRange = species.getDayTimeRange();
        if (dayRange != null && dayRange.length >= 2) {
            text.append("  dayTimeRange=").append(formatFloatRange(dayRange));
            text.append('\n');
        }

        String[] weather = species.getWeatherIds();
        text.append("  weather=").append(weather != null && weather.length > 0 ? Arrays.toString(weather) : "any");
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
        String biome = WaterBodyClassifier.getBiomeName(world, blockX, blockZ);
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
        text.append('\n');
        text.append("  shadowWaterBody=").append(shadow.getWaterBodyType().name());
        text.append(", catchEnvIndex=").append(catchEnvironmentIndex);
        text.append(", playerEnvIndex=").append(playerEnvironmentIndex);
        text.append(", underground=").append(underground);
        text.append('\n');

        if (surfaceY >= 0) {
            boolean envMatch =
                FishShadowSpawnHelper.matchesSpawnEnvironment(
                    species,
                    catchEnvironmentIndex,
                    world,
                    blockX,
                    blockZ,
                    playerEnvironmentIndex
                );
            boolean undergroundMatch = species.isUndergroundOnly() == underground;
            boolean waterBodyMatch = species.matchesWaterBody(shadow.getWaterBodyType());
            text.append("  matches: waterBody=")
                .append(waterBodyMatch)
                .append(", environment=")
                .append(envMatch)
                .append(", underground=")
                .append(undergroundMatch);
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

    @Nonnull
    private static String formatWaterBodyTypes(@Nonnull WaterBodyType[] types) {
        if (types.length == 0) {
            return "[]";
        }
        return Arrays.stream(types).map(WaterBodyType::name).collect(Collectors.joining(", ", "[", "]"));
    }

    @Nonnull
    private static String formatFloatRange(@Nonnull float[] range) {
        return String.format(Locale.US, "[%.1f, %.1f]", range[0], range[1]);
    }
}
