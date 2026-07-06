package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class FishShadowSpawnSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Query<EntityStore> query = Query.and(Player.getComponentType(), TransformComponent.getComponentType());

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        if (!FishShadowProximity.isHoldingFishingRod(commandBuffer, playerRef)) {
            return;
        }

        FishingModConfig config = FishingModConfig.get();
        FishShadowSpawnStateComponent spawnState = commandBuffer.getComponent(playerRef, FishShadowSpawnStateComponent.getComponentType());
        if (spawnState == null) {
            spawnState = new FishShadowSpawnStateComponent();
            spawnState.setStaggerOffsetTicks(Math.abs(playerRef.hashCode()) % Math.max(1, (int) (config.getSpawnCheckIntervalSeconds() * 20)));
            commandBuffer.putComponent(playerRef, FishShadowSpawnStateComponent.getComponentType(), spawnState);
        }

        if (spawnState.getStaggerOffsetTicks() > 0) {
            spawnState.setStaggerOffsetTicks(spawnState.getStaggerOffsetTicks() - 1);
            return;
        }

        spawnState.setSpawnTimer(spawnState.getSpawnTimer() + dt);
        if (spawnState.getSpawnTimer() < config.getSpawnCheckIntervalSeconds()) {
            return;
        }
        spawnState.setSpawnTimer(0.0f);

        FishShadowSpawnDiagnostics.Report report = FishShadowSpawnDiagnostics.begin(config.isEnableSpawnDiagnostics());

        World world = store.getExternalData().getWorld();
        if (world == null) {
            report.skip(FishShadowSpawnDiagnostics.SkipReason.NO_WORLD);
            report.flush();
            return;
        }

        int nearbyShadows = countShadowsNearPlayer(store, playerRef, config.getSpawnRadiusMax());
        if (nearbyShadows >= config.getShadowsPerPlayerCap()) {
            TransformComponent capTransform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
            if (capTransform != null) {
                report.setContext(
                    capTransform.getPosition(),
                    resolvePlayerEnvironmentIndex(commandBuffer, playerRef),
                    WaterBodyClassifier.getBiomeName(world, (int) Math.floor(capTransform.getPosition().x), (int) Math.floor(capTransform.getPosition().z)),
                    nearbyShadows,
                    config.getShadowsPerPlayerCap(),
                    (int) config.getSpawnRadiusMax(),
                    config.getMinWaterDepthBlocks()
                );
            }
            report.skip(FishShadowSpawnDiagnostics.SkipReason.SHADOW_CAP_REACHED);
            report.flush();
            return;
        }

        TransformComponent transform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            report.flush();
            return;
        }

        Vector3d playerPos = transform.getPosition();
        int searchRadius = (int) config.getSpawnRadiusMax();
        int minDepth = Math.min(config.getMinWaterDepthBlocks(), 1);
        FishShadowSpawnHelper.WaterScanResult waterScan =
            FishShadowSpawnHelper.scanWaterColumnsInRadius(world, playerPos, searchRadius, minDepth);
        int playerEnvironmentIndex = resolvePlayerEnvironmentIndex(commandBuffer, playerRef);
        String playerBiome = WaterBodyClassifier.getBiomeName(world, (int) Math.floor(playerPos.x), (int) Math.floor(playerPos.z));

        report.setContext(
            playerPos,
            playerEnvironmentIndex,
            playerBiome,
            nearbyShadows,
            config.getShadowsPerPlayerCap(),
            searchRadius,
            minDepth
        );
        report.setWaterScan(waterScan.shallowColumns(), waterScan.validColumns());

        if (waterScan.validColumns() == 0) {
            report.skip(FishShadowSpawnDiagnostics.SkipReason.NO_WATER_IN_RADIUS);
            report.flush();
            return;
        }

        FishShadowSpawner.trySpawnNearPlayer(commandBuffer, playerRef, world, playerPos, config, playerEnvironmentIndex, report);
        report.flush();
        WaterBodyClassifier.logStatsAndReset();
    }

    private static int resolvePlayerEnvironmentIndex(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> playerRef) {
        WeatherTracker weatherTracker = commandBuffer.getComponent(playerRef, WeatherTracker.getComponentType());
        return weatherTracker != null ? weatherTracker.getEnvironmentId() : -1;
    }

    private static int countShadowsNearPlayer(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef, float radius) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return 0;
        }
        Vector3d pos = transform.getPosition();
        int[] count = {0};
        double radiusSq = radius * radius;
        store.forEachEntityParallel(
            FishShadowComponent.getComponentType(),
            (idx, chunk, ignored) -> {
                TransformComponent shadowTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
                if (shadowTransform == null) {
                    return;
                }
                Vector3d shadowPos = shadowTransform.getPosition();
                double dx = shadowPos.x - pos.x;
                double dz = shadowPos.z - pos.z;
                if (dx * dx + dz * dz <= radiusSq) {
                    count[0]++;
                }
            }
        );
        return count[0];
    }
}
