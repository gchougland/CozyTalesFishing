package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.bobber.BobberDurabilityService;
import com.hexvane.cozytalefishing.fish.FishCatchService;
import com.hexvane.cozytalefishing.fish.FishShadowComponent;
import com.hexvane.cozytalefishing.fish.FishShadowEntityPool;
import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
        FishingLineService.sanitizeStaleLineState(commandBuffer, playerRef);

        FishingLineComponent line = commandBuffer.getComponent(playerRef, FishingLineComponent.getComponentType());
        if (line == null) {
            return;
        }

        if (!line.isActive() && line.isReeling()) {
            line.setReeling(false);
            commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }

        if (!line.isActive()) {
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
        RodTipUtil.getAnchoredRodTipPosition(playerRef, commandBuffer, line, tip);

        FishingBobberComponent bobberState = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        Vector3d bobberPos = new Vector3d();
        TransformComponent bobberTransform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        if (bobberTransform != null) {
            bobberPos.set(bobberTransform.getPosition());
        }
        bobberPos.y = FishingLineService.getBobberWorldY(commandBuffer, bobberRef, bobberState);

        FishingModConfig config = FishingModConfig.get();

        if (line.getPhase() == FishingLinePhase.FIGHTING) {
            tickFightStamina(line, config, dt);
            if (!line.isFightReelingActive()) {
                applyFightLineSlack(commandBuffer, line, dt);
            }
            Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
            PlayerRef universePlayerRef = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
            if (player != null && universePlayerRef != null) {
                FishingFightHudService.update(
                    player,
                    universePlayerRef,
                    line.getFishingStamina(),
                    line.getFishingStaminaMax()
                );
            }
        }

        if (line.isReeling()) {
            if (line.getPhase() == FishingLinePhase.FLOATING || line.getPhase() == FishingLinePhase.REELING) {
                applySlowReel(commandBuffer, line, tip, bobberPos, bobberRef, bobberTransform, config, dt);
                if (line.getMaxLength() <= config.getRecallLineLengthBlocks()) {
                    FishingLineService.recallCastOut(commandBuffer, playerRef);
                    return;
                }
            } else if (line.getPhase() == FishingLinePhase.FIGHTING && line.isFightReelingActive()) {
                applyFightLineReel(line, config, dt);
            }
        }

        if (line.getPhase() == FishingLinePhase.FIGHTING) {
            tryCompleteCatch(commandBuffer, playerRef, line, config);
            if (!line.isActive()) {
                return;
            }
        }

        if (line.getPhase() == FishingLinePhase.CASTING && bobberState != null && bobberState.getPhase() == FishingBobberPhase.FLOATING) {
            line.setPhase(FishingLinePhase.FLOATING);
        }

        Vector3d[] nodes = line.getNodePositions();
        Vector3d[] oldNodes = line.getNodeOldPositions();

        float tipToBobber = (float) tip.distance(bobberPos);
        boolean shortLine = tipToBobber < FishingConstants.SHORT_LINE_STRAIGHTEN_BLOCKS;

        if (shortLine) {
            FishingLineMath.layoutNodesOnLine(nodes, tip, bobberPos, FishingConstants.NODE_COUNT);
            for (int i = 0; i < FishingConstants.NODE_COUNT; i++) {
                oldNodes[i].set(nodes[i]);
            }
        } else {
            float gravity =
                line.getPhase() == FishingLinePhase.FLOATING
                    || line.getPhase() == FishingLinePhase.REELING
                    || line.getPhase() == FishingLinePhase.FIGHTING
                    ? FishingConstants.FLOATING_GRAVITY
                    : FishingConstants.GRAVITY;
            FishingLineMath.integrateVerlet(nodes, oldNodes, FishingConstants.NODE_COUNT, gravity, dt);

            if (line.getWhipTicksRemaining() > 0) {
                applyCastWhip(line, nodes, tip);
                line.setWhipTicksRemaining(line.getWhipTicksRemaining() - 1);
            }

            nodes[0].set(tip);
            nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);

            boolean tighteningLine =
                line.isReeling()
                    || line.getPhase() == FishingLinePhase.REELING
                    || (line.getPhase() == FishingLinePhase.FIGHTING && line.isFightReelingActive());
            float slackFactor = tighteningLine ? FishingConstants.REEL_ROPE_SLACK_FACTOR : FishingConstants.ROPE_SLACK_FACTOR;
            float effectiveMaxLength = tighteningLine ? Math.min(line.getMaxLength(), tipToBobber) : line.getMaxLength();
            float segmentLength = FishingLineMath.ropeSegmentLength(tipToBobber, effectiveMaxLength, slackFactor);
            FishingLineMath.satisfyDistanceConstraints(nodes, FishingConstants.NODE_COUNT, segmentLength, FishingConstants.CONSTRAINT_ITERATIONS);

            if (tighteningLine) {
                FishingLineMath.straightenIntermediateNodes(nodes, tip, bobberPos, FishingConstants.NODE_COUNT, 0.45f);
                FishingLineMath.satisfyDistanceConstraints(nodes, FishingConstants.NODE_COUNT, segmentLength, 4);
            } else if (
                line.getPhase() == FishingLinePhase.FLOATING
                    || line.getPhase() == FishingLinePhase.CASTING
                    || line.getPhase() == FishingLinePhase.FIGHTING
            ) {
                FishingLineMath.straightenIntermediateNodes(
                    nodes,
                    tip,
                    bobberPos,
                    FishingConstants.NODE_COUNT,
                    FishingConstants.FLOATING_ROPE_STRAIGHTEN
                );
                FishingLineMath.satisfyDistanceConstraints(nodes, FishingConstants.NODE_COUNT, segmentLength, 4);
            }

            nodes[0].set(tip);
            nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);
            FishingLineMath.enforceMaxLength(tip, nodes[FishingConstants.NODE_COUNT - 1], line.getMaxLength());

            World world = store.getExternalData().getWorld();
            if (world != null) {
                FishingLineGroundUtil.clampAboveGround(world, nodes, FishingConstants.NODE_COUNT);
                nodes[0].set(tip);
                nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);
            }
        }

        nodes[0].set(tip);
        nodes[FishingConstants.NODE_COUNT - 1].set(bobberPos);

        if (!line.isLoggedStretchDebug()) {
            double lineLength = tip.distance(bobberPos);
            if (lineLength > 1.5) {
                int validSegments = 0;
                for (Ref<EntityStore> segmentRef : line.getSegmentRefs()) {
                    if (segmentRef != null && segmentRef.isValid()) {
                        validSegments++;
                    }
                }
                float debugSegmentLength =
                    FishingLineMath.ropeSegmentLength((float) lineLength, line.getMaxLength());
                FishingDebugLog.info(
                    "String sim stretch: length=%.2f maxLen=%.1f segLen=%.3f fixedVisual=%.2f phase=%s validSegments=%d/%d tip=(%.1f,%.1f,%.1f) bobber=(%.1f,%.1f,%.1f)",
                    lineLength,
                    line.getMaxLength(),
                    debugSegmentLength,
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

    private static void applySlowReel(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishingLineComponent line,
        @Nonnull Vector3d tip,
        @Nonnull Vector3d bobberPos,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nullable TransformComponent bobberTransform,
        @Nonnull FishingModConfig config,
        float dt
    ) {
        float shrink = shrinkLineLength(line, config.getReelSpeedBlocksPerSecond(), dt);

        Vector3d toTip = new Vector3d(tip).sub(bobberPos);
        double distance = toTip.length();
        if (distance > 1.0e-6) {
            toTip.normalize().mul(Math.min(shrink, distance));
            bobberPos.add(toTip);
            if (bobberTransform != null) {
                bobberTransform.setPosition(new Vector3d(bobberPos));
                FishingBobberOrientation.applyUpright(commandBuffer, bobberRef);
            }
        }
    }

    private static void applyFightLineReel(
        @Nonnull FishingLineComponent line,
        @Nonnull FishingModConfig config,
        float dt
    ) {
        float reelSpeed = config.getReelSpeedBlocksPerSecond() / Math.max(0.1f, line.getFightDifficulty());
        float shrink = shrinkLineLength(line, reelSpeed, dt);
        line.addReeledDuringFight(shrink);
    }

    /** Undoes reel progress while the fish pulls away during a fight. */
    private static void applyFightLineSlack(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishingLineComponent line,
        float dt
    ) {
        Ref<EntityStore> shadowRef = line.getHookedShadowRef();
        if (shadowRef == null || !shadowRef.isValid()) {
            return;
        }

        FishShadowComponent shadow = commandBuffer.getComponent(shadowRef, FishShadowComponent.getComponentType());
        if (shadow == null) {
            return;
        }

        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(shadow.getSpeciesId());
        if (species == null) {
            return;
        }

        float difficulty = Math.max(0.1f, line.getFightDifficulty());
        float slackRate = species.getFightSwimSpeed() * difficulty;
        float cap = line.getFightStartMaxLength();
        if (cap <= 0.0f) {
            cap = line.getMaxLength();
        }

        float previous = line.getMaxLength();
        float grow = slackRate * dt;
        line.setMaxLength(Math.min(cap, previous + grow));
        float actualGrow = line.getMaxLength() - previous;
        if (actualGrow > 0.0f) {
            line.setReeledDuringFightBlocks(Math.max(0.0f, line.getReeledDuringFightBlocks() - actualGrow));
        }
    }

    private static void tickFightStamina(
        @Nonnull FishingLineComponent line,
        @Nonnull FishingModConfig config,
        float dt
    ) {
        float max = line.getFishingStaminaMax();
        if (max <= 0.0f) {
            return;
        }

        float difficulty = Math.max(0.1f, line.getFightDifficulty());
        float current = line.getFishingStamina();
        if (line.isReeling()) {
            if (current > 0.0f) {
                current -= config.getFishingStaminaDrainPerSecond() * difficulty * dt;
                current = Math.max(0.0f, current);
            }
        } else {
            current += line.getFishingStaminaRegenPerSecond() * dt;
            current = Math.min(max, current);
        }
        line.setFishingStamina(current);
    }

    /** Returns how much line length was removed this tick. */
    private static float shrinkLineLength(
        @Nonnull FishingLineComponent line,
        float reelSpeedBlocksPerSecond,
        float dt
    ) {
        float previous = line.getMaxLength();
        float shrink = reelSpeedBlocksPerSecond * dt;
        line.setMaxLength(Math.max(0.5f, previous - shrink));
        return previous - line.getMaxLength();
    }

    private static void tryCompleteCatch(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull FishingLineComponent line,
        @Nonnull FishingModConfig config
    ) {
        Ref<EntityStore> shadowRef = line.getHookedShadowRef();
        if (shadowRef == null || !shadowRef.isValid()) {
            return;
        }
        TransformComponent shadowTransform = commandBuffer.getComponent(shadowRef, TransformComponent.getComponentType());
        FishShadowComponent shadow = commandBuffer.getComponent(shadowRef, FishShadowComponent.getComponentType());
        if (shadowTransform == null || shadow == null) {
            return;
        }

        if (line.getMaxLength() > config.getCatchDistanceBlocks()) {
            return;
        }
        if (line.getReeledDuringFightBlocks() < config.getCatchMinReelBlocks()) {
            return;
        }

        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(shadow.getSpeciesId());
        if (species == null) {
            FishingLineService.recallCastOut(commandBuffer, playerRef);
            return;
        }

        float sizeCm = line.getRolledSizeCm() > 0.0f ? line.getRolledSizeCm() : FishCatchService.rollSizeCm(species);
        FishCatchService.completeCatch(commandBuffer, playerRef, species, sizeCm, shadow, shadowTransform.getPosition());
        commandBuffer.run(store -> BobberDurabilityService.applyCatchWear(store, playerRef));
        FishShadowEntityPool.despawn(commandBuffer, shadowRef);
        FishingLineService.recallCastOut(commandBuffer, playerRef);
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
