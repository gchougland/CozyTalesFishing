package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingDebugLog;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Structured spawn-check telemetry for diagnosing natural fish shadow spawning. */
public final class FishShadowSpawnDiagnostics {
    public enum SkipReason {
        NO_WORLD,
        SHADOW_CAP_REACHED,
        NO_WATER_IN_RADIUS,
        NO_WATER_COLUMNS,
        NO_VALID_SPECIES,
        ENTITY_SPAWN_FAILED
    }

    public enum AttemptFailure {
        NO_SPECIES,
        ENTITY_SPAWN_FAILED
    }

    public static final class Report {
        private final boolean enabled;
        private final Map<String, Integer> counters = new LinkedHashMap<>();
        private final StringBuilder attemptLines = new StringBuilder();

        @Nullable
        private Vector3d playerPos;
        private int playerEnvironmentIndex = -1;
        @Nullable
        private String playerBiome;
        private int nearbyShadows;
        private int shadowCap;
        private int searchRadius;
        private int minDepthRequired;
        private int shallowWaterColumns;
        private int validWaterColumns;
        @Nullable
        private SkipReason terminalReason;
        private boolean spawned;

        private Report(boolean enabled) {
            this.enabled = enabled;
        }

        public void setContext(
            @Nonnull Vector3d playerPos,
            int playerEnvironmentIndex,
            @Nullable String playerBiome,
            int nearbyShadows,
            int shadowCap,
            int searchRadius,
            int minDepthRequired
        ) {
            this.playerPos = new Vector3d(playerPos);
            this.playerEnvironmentIndex = playerEnvironmentIndex;
            this.playerBiome = playerBiome;
            this.nearbyShadows = nearbyShadows;
            this.shadowCap = shadowCap;
            this.searchRadius = searchRadius;
            this.minDepthRequired = minDepthRequired;
        }

        public void setWaterScan(int shallowWaterColumns, int validWaterColumns) {
            this.shallowWaterColumns = shallowWaterColumns;
            this.validWaterColumns = validWaterColumns;
        }

        public void skip(@Nonnull SkipReason reason) {
            if (!enabled) {
                return;
            }
            this.terminalReason = reason;
            increment("skip." + reason.name());
        }

        public void markSpawned() {
            if (!enabled) {
                return;
            }
            this.spawned = true;
            this.terminalReason = null;
            increment("success");
        }

        public void recordAttemptFailure(@Nonnull AttemptFailure failure) {
            increment("attempt." + failure.name());
        }

        public void addAttemptDetail(@Nonnull String line) {
            if (!enabled) {
                return;
            }
            if (attemptLines.length() > 0) {
                attemptLines.append('\n');
            }
            attemptLines.append("  ").append(line);
        }

        public void flush() {
            if (!enabled) {
                return;
            }

            if (playerPos != null) {
                FishingDebugLog.info(
                    "Spawn telemetry @ (%.1f, %.1f, %.1f): playerEnv=%d playerBiome=%s nearbyShadows=%d/%d radius=%d minDepth=%d shallowWater=%d validWater=%d",
                    playerPos.x,
                    playerPos.y,
                    playerPos.z,
                    playerEnvironmentIndex,
                    playerBiome != null ? playerBiome : "unknown",
                    nearbyShadows,
                    shadowCap,
                    searchRadius,
                    minDepthRequired,
                    shallowWaterColumns,
                    validWaterColumns
                );
            }

            if (attemptLines.length() > 0) {
                FishingDebugLog.info("Spawn attempt details:\n%s", attemptLines);
            }

            if (spawned) {
                FishingDebugLog.info("Spawn telemetry result: SUCCESS (%s)", formatCounters());
                return;
            }

            if (terminalReason != null) {
                FishingDebugLog.info("Spawn telemetry result: SKIPPED (%s) %s", terminalReason, formatCounters());
                return;
            }

            FishingDebugLog.info("Spawn telemetry result: FAILED %s", formatCounters());
        }

        private void increment(@Nonnull String key) {
            if (!enabled) {
                return;
            }
            counters.merge(key, 1, Integer::sum);
        }

        @Nonnull
        private String formatCounters() {
            if (counters.isEmpty()) {
                return "{}";
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : counters.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
            sb.append('}');
            return sb.toString();
        }
    }

    public static final class SpeciesFilterStats {
        private int bodyPoolSize;
        private int rejectedRules;
        private int eligible;

        public int bodyPoolSize() {
            return bodyPoolSize;
        }

        public int rejectedRules() {
            return rejectedRules;
        }

        public int eligible() {
            return eligible;
        }

        @Nonnull
        public String summarize() {
            return String.format(
                Locale.US,
                "pool=%d eligible=%d rejectedRules=%d",
                bodyPoolSize,
                eligible,
                rejectedRules
            );
        }
    }

    private FishShadowSpawnDiagnostics() {}

    @Nonnull
    public static Report begin(boolean enabled) {
        return new Report(enabled);
    }

    @Nonnull
    public static SpeciesFilterStats analyzeSpeciesFilter(
        @Nonnull WaterBodyType bodyType,
        int environmentIndex,
        int blockX,
        int blockZ,
        int surfaceY,
        @Nonnull World world,
        int playerEnvironmentIndex,
        @Nullable FishingSpawnRegionContext regionContext
    ) {
        SpeciesFilterStats stats = new SpeciesFilterStats();
        stats.bodyPoolSize = FishSpeciesRegistry.getSpeciesForWaterBody(bodyType).size();
        FishingModConfig config = FishingModConfig.get();
        FishShadowSpawnHelper.SpawnConditions spawnConditions =
            FishShadowSpawnHelper.resolveSpawnConditions(world, environmentIndex);
        FishSpawnRulesEvaluator.SpawnEvaluationContext context =
            new FishSpawnRulesEvaluator.SpawnEvaluationContext(
                bodyType,
                environmentIndex,
                blockX,
                blockZ,
                surfaceY,
                world,
                spawnConditions,
                regionContext,
                config
            );

        for (FishSpeciesAsset species : FishSpeciesRegistry.getSpeciesForWaterBody(bodyType)) {
            if (species.isTrash() || species.isTreasure()) {
                continue;
            }
            FishSpawnRulesEvaluator.SpawnRuleResult result =
                FishSpawnRulesEvaluator.evaluate(species, context);
            if (result.eligible()) {
                stats.eligible++;
            } else {
                stats.rejectedRules++;
            }
        }

        return stats;
    }
}
