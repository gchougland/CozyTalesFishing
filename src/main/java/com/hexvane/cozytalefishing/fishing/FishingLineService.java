package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class FishingLineService {
    private FishingLineService() {}

    @Nullable
    public static FishingLineComponent getLine(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> playerRef) {
        return commandBuffer.getComponent(playerRef, FishingLineComponent.getComponentType());
    }

    @Nonnull
    public static FishingLineComponent getOrCreateLine(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        FishingLineComponent line = getLine(commandBuffer, playerRef);
        if (line == null) {
            line = new FishingLineComponent();
            commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }
        return line;
    }

    public static boolean hasActiveLine(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> playerRef) {
        FishingLineComponent line = getLine(commandBuffer, playerRef);
        return line != null && line.isActive();
    }

    /** True when the player has an active line or a bobber entity still owned by them. */
    public static boolean hasCastOut(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> playerRef) {
        if (hasActiveLine(commandBuffer, playerRef)) {
            return true;
        }
        UUIDComponent uuidComponent = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
        return uuidComponent != null && findBobberForOwner(commandBuffer, uuidComponent.getUuid()) != null;
    }

    public static boolean recallCastOut(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        if (!hasCastOut(commandBuffer, playerRef)) {
            return false;
        }

        FishingLineComponent line = getLine(commandBuffer, playerRef);
        UUIDComponent uuidComponent = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
        UUID ownerUuid = uuidComponent != null ? uuidComponent.getUuid() : null;

        Ref<EntityStore> bobberRef = line != null ? line.getBobberRef() : null;
        Ref<EntityStore>[] segmentRefs = copySegmentRefs(line);

        if (line != null) {
            line.reset();
            commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }

        Ref<EntityStore> deferredPlayerRef = playerRef;
        commandBuffer.run(
            store ->
                finishTeardown(
                    store,
                    deferredPlayerRef,
                    bobberRef,
                    segmentRefs,
                    ownerUuid
                )
        );
        FishingDebugLog.info("Recall scheduled");
        return true;
    }

    @Nullable
    private static Ref<EntityStore>[] copySegmentRefs(@Nullable FishingLineComponent line) {
        if (line == null) {
            return null;
        }
        Ref<EntityStore>[] segmentRefs = line.getSegmentRefs();
        @SuppressWarnings("unchecked")
        Ref<EntityStore>[] copy = new Ref[segmentRefs.length];
        System.arraycopy(segmentRefs, 0, copy, 0, segmentRefs.length);
        return copy;
    }

    private static void finishTeardown(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nullable Ref<EntityStore> bobberRef,
        @Nullable Ref<EntityStore>[] segmentRefs,
        @Nullable UUID ownerUuid
    ) {
        int removedSegments = 0;
        if (segmentRefs != null) {
            for (Ref<EntityStore> segmentRef : segmentRefs) {
                if (segmentRef != null && segmentRef.isValid()) {
                    store.removeEntity(segmentRef, RemoveReason.REMOVE);
                    removedSegments++;
                }
            }
        }

        boolean removedBobber = false;
        if (bobberRef != null && bobberRef.isValid()) {
            store.removeEntity(bobberRef, RemoveReason.REMOVE);
            removedBobber = true;
        }

        if (ownerUuid != null) {
            removedBobber |= removeAllOwnedBobbers(store, ownerUuid);
        }

        FishingLineComponent line = store.getComponent(playerRef, FishingLineComponent.getComponentType());
        if (line != null && line.isActive()) {
            line.reset();
            store.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }

        FishingDebugLog.info(
            "Recall complete: removedBobber=%s removedSegments=%d",
            removedBobber,
            removedSegments
        );
    }

    @Nullable
    public static Ref<EntityStore> findBobberForOwner(@Nonnull Store<EntityStore> store, @Nonnull UUID ownerUuid) {
        AtomicReference<Ref<EntityStore>> found = new AtomicReference<>();
        store.forEachEntityParallel(
            FishingBobberComponent.getComponentType(),
            (index, chunk, ignored) -> {
                if (found.get() != null) {
                    return;
                }
                FishingBobberComponent bobber = chunk.getComponent(index, FishingBobberComponent.getComponentType());
                if (bobber != null && ownerUuid.equals(bobber.getOwnerUuid())) {
                    found.set(chunk.getReferenceTo(index));
                }
            }
        );
        return found.get();
    }

    @Nullable
    public static Ref<EntityStore> findBobberForOwner(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull UUID ownerUuid
    ) {
        return findBobberForOwner(commandBuffer.getStore(), ownerUuid);
    }

    @Nullable
    public static Ref<EntityStore> castLine(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        float chargeSeconds
    ) {
        if (hasCastOut(commandBuffer, playerRef)) {
            FishingDebugLog.warn("castLine blocked: player already has a cast out");
            return null;
        }

        UUIDComponent uuidComponent = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            logCastFailure("player missing UUIDComponent");
            return null;
        }

        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(FishingConstants.BOBBER_PROJECTILE_CONFIG_ID);
        if (config == null) {
            logCastFailure("missing projectile config " + FishingConstants.BOBBER_PROJECTILE_CONFIG_ID);
            return null;
        }

        float chargeT = FishingLineMath.clamp(chargeSeconds / FishingConstants.MAX_CHARGE_SECONDS, 0.0f, 1.0f);
        float maxLength = FishingLineMath.lerp(FishingConstants.MIN_CAST_BLOCKS, FishingConstants.MAX_CAST_BLOCKS, chargeT);
        float configLaunchForce = (float) config.getLaunchForce();
        float forceMultiplier =
            FishingLineMath.lerp(FishingConstants.MIN_CAST_FORCE_MULTIPLIER, FishingConstants.MAX_CAST_FORCE_MULTIPLIER, chargeT);
        float launchForce = configLaunchForce * forceMultiplier;

        Vector3d tip = new Vector3d();
        Vector3d direction = new Vector3d();
        RodTipUtil.getRodTipPosition(playerRef, commandBuffer, tip);
        RodTipUtil.getCastDirection(playerRef, commandBuffer, direction);

        FishingDebugLog.info(
            "castLine start: charge=%.3fs chargeT=%.2f maxLen=%.1f launchForce=%.1f (config=%.1f x%.2f)",
            chargeSeconds,
            chargeT,
            maxLength,
            launchForce,
            configLaunchForce,
            forceMultiplier
        );

        Ref<EntityStore> bobberRef;
        try {
            bobberRef = ProjectileModule.get().spawnProjectile(playerRef, commandBuffer, config, tip, direction);
        } catch (RuntimeException ex) {
            logCastFailure("spawnProjectile threw: " + ex.getMessage());
            return null;
        }
        if (bobberRef == null) {
            logCastFailure("spawnProjectile failed for " + FishingConstants.BOBBER_PROJECTILE_CONFIG_ID);
            return null;
        }

        FishingLineComponent line = getOrCreateLine(commandBuffer, playerRef);
        line.reset();
        line.setPhase(FishingLinePhase.CASTING);
        line.setChargeNormalized(chargeT);
        line.setMaxLength(maxLength);
        line.setWhipTicksRemaining(FishingConstants.CAST_WHIP_TICKS);
        line.setBobberRef(bobberRef);
        line.setCastSetupPending(true);
        initializeNodes(line, tip, tip);
        commandBuffer.putComponent(playerRef, FishingLineComponent.getComponentType(), line);

        Vector3d tipCopy = new Vector3d(tip);
        Vector3d directionCopy = new Vector3d(direction);
        UUID ownerUuid = uuidComponent.getUuid();
        Ref<EntityStore> deferredPlayerRef = playerRef;
        Ref<EntityStore> deferredBobberRef = bobberRef;

        commandBuffer.run(
            store ->
                completeCastLine(
                    store,
                    deferredPlayerRef,
                    deferredBobberRef,
                    ownerUuid,
                    tipCopy,
                    directionCopy,
                    launchForce,
                    chargeT,
                    maxLength
                )
        );
        return bobberRef;
    }

    private static void completeCastLine(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nonnull UUID ownerUuid,
        @Nonnull Vector3d tip,
        @Nonnull Vector3d direction,
        float launchForce,
        float chargeT,
        float maxLength
    ) {
        FishingLineComponent line = store.getComponent(playerRef, FishingLineComponent.getComponentType());
        if (line == null || !line.isCastSetupPending()) {
            return;
        }

        if (!bobberRef.isValid()) {
            FishingDebugLog.warn("castLine deferred setup failed: bobber ref invalid");
            line.reset();
            store.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
            return;
        }

        applyLaunchForce(store, bobberRef, direction, launchForce);
        store.putComponent(bobberRef, FishingBobberComponent.getComponentType(), new FishingBobberComponent(ownerUuid));

        TransformComponent bobberTransform = store.getComponent(bobberRef, TransformComponent.getComponentType());
        Vector3d bobberPos = bobberTransform != null ? new Vector3d(bobberTransform.getPosition()) : new Vector3d(tip);

        Vector3d layoutBobber = new Vector3d(bobberPos);
        if (tip.distanceSquared(bobberPos) < 0.04) {
            layoutBobber.set(direction).mul(0.75).add(tip);
        }
        initializeNodes(line, tip, layoutBobber);
        Ref<EntityStore> liveBobberRef = findBobberForOwner(store, ownerUuid);
        if (liveBobberRef != null) {
            line.setBobberRef(liveBobberRef);
        }
        line.setCastSetupPending(false);
        store.putComponent(playerRef, FishingLineComponent.getComponentType(), line);

        FishingDebugLog.info(
            "castLine success: chargeT=%.2f maxLen=%.1f launchForce=%.1f bobberPos=(%.2f, %.2f, %.2f) segments=deferred/%d",
            chargeT,
            maxLength,
            launchForce,
            bobberPos.x,
            bobberPos.y,
            bobberPos.z,
            FishingConstants.SEGMENT_COUNT
        );
    }

    private static void applyLaunchForce(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nonnull Vector3d direction,
        float desiredForce
    ) {
        if (desiredForce <= 0.0f) {
            return;
        }
        Vector3d velocity = new Vector3d(direction);
        if (velocity.lengthSquared() > 1.0e-8) {
            velocity.normalize();
        }
        velocity.mul(desiredForce);

        Velocity velocityComponent = store.getComponent(bobberRef, Velocity.getComponentType());
        if (velocityComponent != null) {
            velocityComponent.set(velocity);
        }

        StandardPhysicsProvider physics = store.getComponent(bobberRef, StandardPhysicsProvider.getComponentType());
        if (physics != null) {
            // spawnProjectile seeds nextTickVelocity from config LaunchForce; first physics tick
            // overwrites Velocity unless we replace that pending impulse here.
            physics.getForceProviderStandardState().nextTickVelocity.set(velocity);
            physics.getVelocity().set(velocity);
            physics.setState(StandardPhysicsProvider.STATE.ACTIVE);
        }
    }

    private static void initializeNodes(@Nonnull FishingLineComponent line, @Nonnull Vector3d tip, @Nonnull Vector3d bobber) {
        Vector3d[] nodes = line.getNodePositions();
        Vector3d[] oldNodes = line.getNodeOldPositions();
        for (int i = 0; i < FishingConstants.NODE_COUNT; i++) {
            double t = i / (double) (FishingConstants.NODE_COUNT - 1);
            nodes[i].x = tip.x + (bobber.x - tip.x) * t;
            nodes[i].y = tip.y + (bobber.y - tip.y) * t;
            nodes[i].z = tip.z + (bobber.z - tip.z) * t;
            oldNodes[i].set(nodes[i]);
        }
    }

    public static void teardownLine(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        FishingLineComponent line = store.getComponent(playerRef, FishingLineComponent.getComponentType());
        UUIDComponent uuidComponent = store.getComponent(playerRef, UUIDComponent.getComponentType());
        UUID ownerUuid = uuidComponent != null ? uuidComponent.getUuid() : null;
        Ref<EntityStore> bobberRef = line != null ? line.getBobberRef() : null;
        Ref<EntityStore>[] segmentRefs = copySegmentRefs(line);

        if (line != null) {
            line.reset();
            store.putComponent(playerRef, FishingLineComponent.getComponentType(), line);
        }

        finishTeardown(store, playerRef, bobberRef, segmentRefs, ownerUuid);
    }

    public static void teardownLine(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        recallCastOut(commandBuffer, playerRef);
    }

    private static boolean removeAllOwnedBobbers(@Nonnull Store<EntityStore> store, @Nonnull UUID ownerUuid) {
        boolean[] removed = {false};
        store.forEachEntityParallel(
            FishingBobberComponent.getComponentType(),
            (index, chunk, ignored) -> {
                FishingBobberComponent bobber = chunk.getComponent(index, FishingBobberComponent.getComponentType());
                if (bobber == null || !ownerUuid.equals(bobber.getOwnerUuid())) {
                    return;
                }
                Ref<EntityStore> ref = chunk.getReferenceTo(index);
                if (ref.isValid()) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                    removed[0] = true;
                }
            }
        );
        return removed[0];
    }

    public static void updateSegmentVisuals(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull FishingLineComponent line
    ) {
        Vector3d[] nodes = line.getNodePositions();
        Ref<EntityStore>[] segmentRefs = line.getSegmentRefs();
        Vector3d dir = new Vector3d();
        Rotation3f rotation = new Rotation3f();
        Vector3d position = new Vector3d();
        Vector3d rodTip = new Vector3d();

        for (int i = 0; i < FishingConstants.SEGMENT_COUNT; i++) {
            Vector3d end = nodes[i + 1];
            Vector3d start = nodes[i];
            if (i == 0) {
                RodTipUtil.getRodTipPosition(playerRef, commandBuffer, rodTip);
                start = rodTip;
            }

            dir.set(end).sub(start);
            float dist = (float) dir.length();
            if (dist < 1.0e-4f) {
                dir.set(0, 1, 0);
                dist = 0.01f;
            } else {
                dir.div(dist);
            }
            FishingLineMath.rotationFromDirection(dir, rotation);

            if (i == 0) {
                position.set(start);
            } else if (dist <= FishingConstants.BASE_SEGMENT_LENGTH) {
                float centerOffset = (dist - FishingConstants.BASE_SEGMENT_LENGTH) * 0.5f;
                position.set(start).add(dir.x * centerOffset, dir.y * centerOffset, dir.z * centerOffset);
            } else {
                position
                    .set(start)
                    .sub(
                        dir.x * FishingConstants.SEGMENT_JOINT_OVERLAP,
                        dir.y * FishingConstants.SEGMENT_JOINT_OVERLAP,
                        dir.z * FishingConstants.SEGMENT_JOINT_OVERLAP
                    );
            }

            Ref<EntityStore> segmentRef = segmentRefs[i];
            if (segmentRef == null || !segmentRef.isValid()) {
                segmentRef = SegmentEntityPool.spawnSegment(commandBuffer, position, rotation, i);
                segmentRefs[i] = segmentRef;
                // Ref from commandBuffer.addEntity is not valid until the buffer flushes; spawn already set pose.
                continue;
            }
            SegmentEntityPool.updateSegment(commandBuffer, segmentRef, position, rotation);
        }
    }

    public static double getBobberWorldY(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nullable FishingBobberComponent bobberState
    ) {
        TransformComponent transform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        if (transform == null) {
            return 0.0;
        }
        double y = transform.getPosition().y;
        if (bobberState != null && bobberState.getPhase() == FishingBobberPhase.FLOATING) {
            y =
                bobberState.getLatchedSurfaceY()
                    + Math.sin(bobberState.getBobPhase()) * FishingConstants.BOB_AMPLITUDE;
        }
        return y;
    }

    private static void logCastFailure(@Nonnull String reason) {
        FishingDebugLog.warn("castLine failed: %s", reason);
    }
}
