package com.hexvane.cozytalefishing.fishing;

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

/** Verlet simulation and segment prop placement for active fishing lines. */
public final class FishingLineTickSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Query<EntityStore> query =
        Query.and(Player.getComponentType(), FishingLineComponent.getComponentType());

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
        FishingLineComponent line = archetypeChunk.getComponent(index, FishingLineComponent.getComponentType());
        if (line == null || !line.isActive()) {
            return;
        }

        Ref<EntityStore> bobberRef = line.getBobberRef();
        if (bobberRef == null || !bobberRef.isValid()) {
            if (line.isCastSetupPending()) {
                return;
            }
            FishingLineService.teardownLine(commandBuffer, playerRef);
            return;
        }

        Vector3d tip = new Vector3d();
        RodTipUtil.getRodTipPosition(playerRef, commandBuffer, tip);

        FishingBobberComponent bobberState = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        Vector3d bobberPos = new Vector3d();
        TransformComponent bobberTransform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        if (bobberTransform != null) {
            bobberPos.set(bobberTransform.getPosition());
        }
        bobberPos.y = FishingLineService.getBobberWorldY(commandBuffer, bobberRef, bobberState);

        if (line.getPhase() == FishingLinePhase.CASTING && bobberState != null && bobberState.getPhase() == FishingBobberPhase.FLOATING) {
            line.setPhase(FishingLinePhase.FLOATING);
        }

        Vector3d[] nodes = line.getNodePositions();
        Vector3d[] oldNodes = line.getNodeOldPositions();

        float gravity =
            line.getPhase() == FishingLinePhase.FLOATING ? FishingConstants.FLOATING_GRAVITY : FishingConstants.GRAVITY;
        FishingLineMath.integrateVerlet(nodes, oldNodes, FishingConstants.NODE_COUNT, gravity, dt);

        if (line.getWhipTicksRemaining() > 0) {
            applyCastWhip(line, nodes, tip);
            line.setWhipTicksRemaining(line.getWhipTicksRemaining() - 1);
        }

        nodes[0].set(tip);
        nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);

        float tipToBobber = (float) tip.distance(bobberPos);
        float segmentLength = FishingLineMath.ropeSegmentLength(tipToBobber, line.getMaxLength());
        FishingLineMath.satisfyDistanceConstraints(nodes, FishingConstants.NODE_COUNT, segmentLength, FishingConstants.CONSTRAINT_ITERATIONS);

        nodes[0].set(tip);
        nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);
        FishingLineMath.enforceMaxLength(tip, nodes[FishingConstants.NODE_COUNT - 1], line.getMaxLength());

        World world = store.getExternalData().getWorld();
        if (world != null) {
            FishingLineGroundUtil.clampAboveGround(world, nodes, FishingConstants.NODE_COUNT);
            nodes[0].set(tip);
            nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);
        }

        if (!line.isLoggedStretchDebug()) {
            double lineLength = tip.distance(bobberPos);
            if (lineLength > 1.5) {
                int validSegments = 0;
                for (Ref<EntityStore> segmentRef : line.getSegmentRefs()) {
                    if (segmentRef != null && segmentRef.isValid()) {
                        validSegments++;
                    }
                }
                FishingDebugLog.info(
                    "String sim stretch: length=%.2f maxLen=%.1f segLen=%.3f fixedVisual=%.2f phase=%s validSegments=%d/%d tip=(%.1f,%.1f,%.1f) bobber=(%.1f,%.1f,%.1f)",
                    lineLength,
                    line.getMaxLength(),
                    segmentLength,
                    FishingConstants.BASE_SEGMENT_LENGTH,
                    line.getPhase(),
                    validSegments,
                    FishingConstants.SEGMENT_COUNT,
                    tip.x,
                    tip.y,
                    tip.z,
                    bobberPos.x,
                    bobberPos.y,
                    bobberPos.z
                );
                line.setLoggedStretchDebug(true);
            }
        }

        FishingLineService.updateSegmentVisuals(commandBuffer, playerRef, line);
    }

    private static void applyCastWhip(@Nonnull FishingLineComponent line, @Nonnull Vector3d[] nodes, @Nonnull Vector3d tip) {
        Ref<EntityStore> bobberRef = line.getBobberRef();
        if (bobberRef == null) {
            return;
        }
        Vector3d impulseDir = new Vector3d(nodes[FishingConstants.NODE_COUNT - 1]).sub(tip);
        if (impulseDir.lengthSquared() < 1.0e-8) {
            return;
        }
        impulseDir.normalize().mul(FishingConstants.CAST_WHIP_IMPULSE);
        int whipNodes = Math.min(4, FishingConstants.NODE_COUNT - 2);
        for (int i = 1; i <= whipNodes; i++) {
            nodes[i].add(impulseDir);
        }
    }
}
