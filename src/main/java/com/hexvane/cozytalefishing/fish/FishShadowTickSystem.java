package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingBobberComponent;
import com.hexvane.cozytalefishing.fishing.FishingBobberPhase;
import com.hexvane.cozytalefishing.fishing.FishingLineComponent;
import com.hexvane.cozytalefishing.fishing.FishingLinePhase;
import com.hexvane.cozytalefishing.fishing.FishingLineService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class FishShadowTickSystem extends EntityTickingSystem<EntityStore> {
    private static final float POKE_LURK_DISTANCE = 1.65f;
    private static final float POKE_LURK_TIME_MIN = 0.55f;
    private static final float POKE_LURK_TIME_MAX = 1.05f;
    private static final float POKE_LUNGE_SPEED_FACTOR = 2.8f;
    private static final float POKE_RETREAT_SPEED_FACTOR = 2.0f;
    /** Splash VFX/SFX interval while fighting and reeling. */
    private static final float FIGHT_SPLASH_INTERVAL = 0.35f;
    /** Fish swims to the floating bobber before the fight starts. */
    private static final float HOOK_APPROACH_SPEED_FACTOR = 2.4f;
    /** Retreat phase ends if the shadow cannot move away from shore for this long. */
    private static final float POKE_RETREAT_STUCK_SECONDS = 0.45f;

    @Nonnull
    private final Query<EntityStore> query =
        Query.and(FishShadowComponent.getComponentType(), TransformComponent.getComponentType());

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
        Ref<EntityStore> shadowRef = archetypeChunk.getReferenceTo(index);
        FishShadowComponent shadow = archetypeChunk.getComponent(index, FishShadowComponent.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (shadow == null || transform == null) {
            return;
        }

        World world = store.getExternalData().getWorld();

        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(shadow.getSpeciesId());
        if (species == null) {
            FishShadowEntityPool.despawn(commandBuffer, shadowRef);
            return;
        }

        if (sanitizeBrokenShadow(commandBuffer, shadowRef, shadow)) {
            return;
        }

        Vector3d position = new Vector3d(transform.getPosition());

        switch (shadow.getState()) {
            case WANDERING -> tickWandering(commandBuffer, shadowRef, shadow, species, world, position, dt);
            case ALERT -> tickAlert(commandBuffer, shadowRef, shadow, species, position, dt);
            case POKING -> tickPoking(commandBuffer, shadowRef, shadow, species, position, dt);
            case HOOKED -> tickHooked(commandBuffer, shadowRef, shadow, species, world, position, dt);
            case FIGHTING -> tickFighting(commandBuffer, shadowRef, shadow, species, world, position, dt);
            case FLEEING -> tickFleeing(commandBuffer, shadowRef, shadow, species, world, position, dt);
            case DESPAWNING -> FishShadowEntityPool.despawn(commandBuffer, shadowRef);
        }

        persistShadow(commandBuffer, shadowRef, shadow);
    }

    private static void persistShadow(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow
    ) {
        commandBuffer.putComponent(shadowRef, FishShadowComponent.getComponentType(), shadow);
    }

    private static void tickWandering(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nullable World world,
        @Nonnull Vector3d position,
        float dt
    ) {
        if (tryFleeFromNearbyPlayer(commandBuffer, shadowRef, shadow, species, world, position)) {
            return;
        }

        FishingModConfig config = FishingModConfig.get();
        float idleDespawnSeconds = config.getShadowIdleDespawnSeconds();
        if (idleDespawnSeconds > 0.0f) {
            if (FishShadowProximity.hasRodHolderNear(commandBuffer, position, config.getSpawnRadiusMax())) {
                shadow.setIdleTimer(0.0f);
            } else {
                shadow.setIdleTimer(shadow.getIdleTimer() + dt);
                if (shadow.getIdleTimer() >= idleDespawnSeconds) {
                    startIdleDespawnFlee(shadow);
                    tickFleeing(commandBuffer, shadowRef, shadow, species, world, position, dt);
                    return;
                }
            }
        }

        Ref<EntityStore> bobberRef = findBobberInVision(commandBuffer, shadow, species, position);
        if (bobberRef != null) {
            beginPokeSequence(commandBuffer, shadow, bobberRef);
            Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
            float yaw = FishShadowVision.swimYaw(
                (float) (bobberPos.x - position.x),
                (float) (bobberPos.z - position.z)
            );
            FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, yaw, shadow.getCurrentScale());
            return;
        }

        shadow.setWanderTimer(shadow.getWanderTimer() - dt);
        if (shadow.getWanderTimer() <= 0.0f) {
            shadow.setWanderDirection((float) (Math.random() * Math.PI * 2.0));
            shadow.setWanderTimer(3.0f + (float) Math.random() * 5.0f);
        }

        float speed = species.getSwimSpeed() * dt;
        if (!FishShadowSpawnHelper.tryMoveOnWaterSurface(world, position, Math.sin(shadow.getWanderDirection()) * speed, Math.cos(shadow.getWanderDirection()) * speed)) {
            shadow.setWanderDirection((float) (Math.random() * Math.PI * 2.0));
            shadow.setWanderTimer(1.0f + (float) Math.random() * 2.0f);
        }
        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, shadow.getWanderDirection(), shadow.getCurrentScale());
    }

    private static void tickAlert(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nonnull Vector3d position,
        float dt
    ) {
        shadow.setState(FishShadowState.POKING);
        tickPoking(commandBuffer, shadowRef, shadow, species, position, dt);
    }

    private static void tickPoking(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nonnull Vector3d position,
        float dt
    ) {
        Ref<EntityStore> bobberRef = resolveTargetBobber(commandBuffer, shadow, position, species.getVisionRange() * 1.5f);
        if (bobberRef == null) {
            clearBobberTarget(shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
        if (!shadow.isBobberAnchorInitialized()) {
            shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
        }

        if (isOwnerReelingDuringPoke(commandBuffer, shadow, bobberRef)) {
            startScaredFleeFromBobber(shadow, position, bobberPos);
            if (!FishShadowSpawnHelper.tryMoveOnWaterSurface(
                world,
                position,
                Math.sin(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.4f * dt,
                Math.cos(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.4f * dt
            )) {
                shadow.setWanderDirection((float) (Math.random() * Math.PI * 2.0));
            }
            FishShadowEntityPool.updateTransform(
                commandBuffer,
                shadowRef,
                position,
                shadow.getWanderDirection(),
                shadow.getCurrentScale()
            );
            return;
        }

        double dx = bobberPos.x - position.x;
        double dz = bobberPos.z - position.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float lurkDistance = POKE_LURK_DISTANCE;
        float pokeReach = species.getPokeReachBlocks();
        float swimSpeed = species.getSwimSpeed();
        float yaw;

        switch (shadow.getPokePhase()) {
            case LURK -> {
                Vector3d lurkTarget = lurkPositionAround(bobberPos, position, lurkDistance);
                boolean moved = swimTowardTarget(world, position, lurkTarget, swimSpeed * 0.75f, dt);
                dx = bobberPos.x - position.x;
                dz = bobberPos.z - position.z;
                dist = Math.sqrt(dx * dx + dz * dz);
                yaw = yawWhileSwimming(position, lurkTarget);
                shadow.setStateTimer(shadow.getStateTimer() - dt);
                if (shadow.getStateTimer() <= 0.0f && dist <= lurkDistance * 1.2f) {
                    boolean inLurkBand = dist >= lurkDistance * 0.65f;
                    if (inLurkBand || !moved || dist > pokeReach) {
                        shadow.setPokePhase(FishShadowPokePhase.LUNGE);
                    }
                }
            }
            case LUNGE -> {
                boolean moved = swimTowardTarget(world, position, bobberPos, swimSpeed * POKE_LUNGE_SPEED_FACTOR, dt);
                dx = bobberPos.x - position.x;
                dz = bobberPos.z - position.z;
                dist = Math.sqrt(dx * dx + dz * dz);
                yaw = FishShadowVision.swimYaw((float) dx, (float) dz);
                if (dist <= pokeReach || (!moved && dist <= lurkDistance * 1.2f)) {
                    FishShadowEffects.playPoke(commandBuffer, bobberPos);
                    shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
                    shadow.setPokePhase(FishShadowPokePhase.RETREAT);
                    shadow.setStateTimer(0.0f);
                }
            }
            case RETREAT -> {
                double distBefore = dist;
                boolean moved = swimAwayFromTarget(world, position, bobberPos, swimSpeed * POKE_RETREAT_SPEED_FACTOR, dt);
                dx = bobberPos.x - position.x;
                dz = bobberPos.z - position.z;
                dist = Math.sqrt(dx * dx + dz * dz);
                yaw = FishShadowVision.swimYaw((float) -dx, (float) -dz);
                if (dist >= lurkDistance * 0.9f) {
                    finishPokeRetreat(commandBuffer, shadowRef, shadow, species, bobberPos);
                } else if (!moved || dist <= distBefore + 1.0e-4) {
                    shadow.setStateTimer(shadow.getStateTimer() + dt);
                    if (shadow.getStateTimer() >= POKE_RETREAT_STUCK_SECONDS) {
                        finishPokeRetreat(commandBuffer, shadowRef, shadow, species, bobberPos);
                    }
                } else {
                    shadow.setStateTimer(0.0f);
                }
            }
            default -> {
                shadow.setPokePhase(FishShadowPokePhase.LURK);
                yaw = shadow.getWanderDirection();
            }
        }

        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, yaw, shadow.getCurrentScale());
    }

    private static void beginPokeSequence(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Ref<EntityStore> bobberRef
    ) {
        assignTargetBobber(commandBuffer, shadow, bobberRef);
        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
        shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
        shadow.setState(FishShadowState.POKING);
        shadow.setPokePhase(FishShadowPokePhase.LURK);
        int pokeCount = 1 + (int) (Math.random() * 4.0);
        shadow.setPokeTotal(pokeCount);
        shadow.setPokeCountRemaining(pokeCount);
        shadow.setStateTimer(POKE_LURK_TIME_MIN + (float) Math.random() * (POKE_LURK_TIME_MAX - POKE_LURK_TIME_MIN));
        shadow.setPokeTimer(0.0f);
        shadow.setIdleTimer(0.0f);
    }

    private static void commitHookPullUnder(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species
    ) {
        Ref<EntityStore> bobberRef = resolveHookBobber(commandBuffer, shadow);
        if (bobberRef == null) {
            clearBobberTarget(shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
        shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
        assignTargetBobber(commandBuffer, shadow, bobberRef);
        shadow.setState(FishShadowState.HOOKED);
        shadow.setIdleTimer(0.0f);
        shadow.setPokeTimer(0.0f);
    }

    private static void finishPokeRetreat(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nonnull Vector3d bobberPos
    ) {
        if (shadow.getPokeCountRemaining() > 0) {
            shadow.setPokeCountRemaining(shadow.getPokeCountRemaining() - 1);
        }
        if (shadow.getPokeCountRemaining() <= 0) {
            commitHookPullUnder(commandBuffer, shadowRef, shadow, species);
        } else {
            shadow.setPokePhase(FishShadowPokePhase.LURK);
            shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
            shadow.setStateTimer(POKE_LURK_TIME_MIN + (float) Math.random() * (POKE_LURK_TIME_MAX - POKE_LURK_TIME_MIN));
            shadow.setPokeTimer(0.0f);
        }
    }

    private static void tickHooked(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nullable World world,
        @Nonnull Vector3d position,
        float dt
    ) {
        Ref<EntityStore> bobberRef = shadow.getTargetBobberRef();
        if (bobberRef == null || !bobberRef.isValid()) {
            clearBobberTarget(shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null || bobber.getPhase() != FishingBobberPhase.FLOATING) {
            clearBobberTarget(shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);

        double dx = bobberPos.x - position.x;
        double dz = bobberPos.z - position.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float pokeReach = species.getPokeReachBlocks();
        boolean moved = swimTowardTarget(world, position, bobberPos, species.getSwimSpeed() * HOOK_APPROACH_SPEED_FACTOR, dt);
        dx = bobberPos.x - position.x;
        dz = bobberPos.z - position.z;
        dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = FishShadowVision.swimYaw((float) dx, (float) dz);
        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, yaw, shadow.getCurrentScale());

        if (dist <= pokeReach) {
            beginFight(commandBuffer, shadowRef, shadow, species);
        } else if (!moved) {
            shadow.setPokeTimer(shadow.getPokeTimer() + dt);
            if (shadow.getPokeTimer() >= POKE_RETREAT_STUCK_SECONDS || dist <= POKE_LURK_DISTANCE * 1.2f) {
                beginFight(commandBuffer, shadowRef, shadow, species);
            }
        } else {
            shadow.setPokeTimer(0.0f);
        }
    }

    @Nullable
    private static Ref<EntityStore> resolveHookBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow
    ) {
        Ref<EntityStore> bobberRef = shadow.getTargetBobberRef();
        if (isFloatingBobber(commandBuffer, bobberRef)) {
            return bobberRef;
        }

        UUID ownerUuid = shadow.getTargetBobberOwnerUuid();
        if (ownerUuid != null) {
            bobberRef = FishingLineService.findBobberForOwner(commandBuffer, ownerUuid);
            if (isFloatingBobber(commandBuffer, bobberRef)) {
                shadow.setTargetBobberRef(bobberRef);
                return bobberRef;
            }
        }
        return null;
    }

    private static boolean isFloatingBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nullable Ref<EntityStore> bobberRef
    ) {
        if (bobberRef == null || !bobberRef.isValid()) {
            return false;
        }
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        return bobber != null && bobber.getPhase() == FishingBobberPhase.FLOATING;
    }

    private static void pinBobberSubmerged(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nullable FishingBobberComponent bobber
    ) {
        TransformComponent bobberTransform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        if (bobberTransform == null) {
            return;
        }
        double anchorX = shadow.isBobberAnchorInitialized() ? shadow.getBobberAnchorX() : bobberTransform.getPosition().x;
        double anchorZ = shadow.isBobberAnchorInitialized() ? shadow.getBobberAnchorZ() : bobberTransform.getPosition().z;
        double submergedY = bobber != null ? bobber.getLatchedSurfaceY() - 0.45 : bobberTransform.getPosition().y - 0.45;
        bobberTransform.setPosition(new Vector3d(anchorX, submergedY, anchorZ));
    }

    private static void beginFight(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species
    ) {
        Ref<EntityStore> bobberRef = resolveHookBobber(commandBuffer, shadow);
        if (bobberRef == null) {
            clearBobberTarget(shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null) {
            clearBobberTarget(shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
        if (!bobber.isSubmerged()) {
            bobber.setSubmerged(true);
            FishShadowEffects.playPullUnder(commandBuffer, bobberPos);
        }
        bobber.setHookedShadowRef(shadowRef);
        UUID ownerUuid = bobber.getOwnerUuid();
        shadow.setHookedPlayerUuid(ownerUuid);
        PlayerRef playerRef = Universe.get().getPlayer(ownerUuid);
        if (playerRef != null) {
            Ref<EntityStore> ownerEntity = playerRef.getReference();
            if (ownerEntity != null && ownerEntity.isValid()) {
                FishingLineComponent line = commandBuffer.getComponent(ownerEntity, FishingLineComponent.getComponentType());
                if (line != null) {
                    line.setPhase(FishingLinePhase.FIGHTING);
                    line.setHookedShadowRef(shadowRef);
                    line.setRolledSizeCm(FishCatchService.rollSizeCm(species));
                    line.setFightStartMaxLength(line.getMaxLength());
                    line.setReeledDuringFightBlocks(0.0f);
                    commandBuffer.putComponent(ownerEntity, FishingLineComponent.getComponentType(), line);
                }
            }
        }
        commandBuffer.putComponent(bobberRef, FishingBobberComponent.getComponentType(), bobber);

        pinBobberSubmerged(commandBuffer, shadow, bobberRef, bobber);
        shadow.setState(FishShadowState.FIGHTING);
        shadow.setStateTimer(FIGHT_SPLASH_INTERVAL * 0.5f);
    }

    private static void tickFighting(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nullable World world,
        @Nonnull Vector3d position,
        float dt
    ) {
        UUID ownerUuid = shadow.getHookedPlayerUuid();
        Ref<EntityStore> ownerEntity = findPlayerRef(ownerUuid);
        if (ownerEntity == null) {
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        TransformComponent ownerTransform = commandBuffer.getComponent(ownerEntity, TransformComponent.getComponentType());
        if (ownerTransform == null) {
            return;
        }

        Vector3d ownerPos = ownerTransform.getPosition();
        FishingLineComponent line = commandBuffer.getComponent(ownerEntity, FishingLineComponent.getComponentType());
        boolean reeling = line != null && line.isReeling();
        FishingModConfig config = FishingModConfig.get();

        float fleeSpeed = species.getFightSwimSpeed();
        float pullSpeed = config.getFightReelSpeedBlocksPerSecond();
        float resist = fleeSpeed * config.getFightReelResistanceFactor();

        Vector3d moveDir = new Vector3d();
        float moveSpeed;
        if (reeling) {
            moveDir.set(ownerPos.x - position.x, 0.0, ownerPos.z - position.z);
            moveSpeed = Math.max(0.85f, pullSpeed - resist);
        } else {
            moveDir.set(position.x - ownerPos.x, 0.0, position.z - ownerPos.z);
            moveSpeed = fleeSpeed;
        }

        float yaw = shadow.getWanderDirection();
        if (moveDir.lengthSquared() > 1.0e-6) {
            moveDir.normalize();
            double step = moveSpeed * dt;
            if (!FishShadowSpawnHelper.tryMoveOnWaterSurface(world, position, moveDir.x * step, moveDir.z * step)) {
                tryStrafeFightMove(world, position, moveDir, (float) step);
            }
            yaw = FishShadowVision.swimYaw((float) moveDir.x, (float) moveDir.z);
        } else {
            snapShadowToWaterSurface(world, position);
        }

        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, yaw, shadow.getCurrentScale());

        if (reeling) {
            shadow.setStateTimer(shadow.getStateTimer() - dt);
            if (shadow.getStateTimer() <= 0.0f) {
                FishShadowEffects.playFightSplash(commandBuffer, position);
                shadow.setStateTimer(FIGHT_SPLASH_INTERVAL);
            }
        }

        Ref<EntityStore> bobberRef = shadow.getTargetBobberRef();
        if (bobberRef != null && bobberRef.isValid()) {
            TransformComponent bobberTransform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
            FishingBobberComponent bobberState = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
            if (bobberTransform != null) {
                int surfaceBlockY = FishShadowSpawnHelper.findSurfaceWaterBlockY(world, (int) Math.floor(position.x), (int) Math.floor(position.z));
                double surfaceWorldY =
                    surfaceBlockY >= 0 ? FishShadowSpawnHelper.waterSurfaceWorldY(surfaceBlockY) : position.y;
                double submergedY = surfaceWorldY - 0.45;
                if (bobberState != null && bobberState.getPhase() == FishingBobberPhase.FLOATING) {
                    submergedY = bobberState.getLatchedSurfaceY() - 0.45;
                }
                Vector3d bobberPos = bobberTransform.getPosition();
                float followLerp = Math.min(1.0f, 5.0f * dt);
                double newX = bobberPos.x + (position.x - bobberPos.x) * followLerp;
                double newZ = bobberPos.z + (position.z - bobberPos.z) * followLerp;
                bobberTransform.setPosition(new Vector3d(newX, submergedY, newZ));
            }
        }
    }

    private static void tickFleeing(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nullable World world,
        @Nonnull Vector3d position,
        float dt
    ) {
        FishingModConfig config = FishingModConfig.get();
        shadow.setFleeTimer(shadow.getFleeTimer() + dt);
        float progress = shadow.getFleeTimer() / config.getFleeShrinkDurationSeconds();
        if (progress >= 1.0f) {
            shadow.setState(FishShadowState.DESPAWNING);
            return;
        }

        float scale = shadow.getBaseScale() * Math.max(0.01f, 1.0f - progress);
        if (!FishShadowSpawnHelper.tryMoveOnWaterSurface(
            world,
            position,
            Math.sin(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.5f * dt,
            Math.cos(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.5f * dt
        )) {
            shadow.setWanderDirection((float) (Math.random() * Math.PI * 2.0));
        }
        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, shadow.getWanderDirection(), scale);
    }

    private static boolean tryFleeFromNearbyPlayer(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nullable World world,
        @Nonnull Vector3d position
    ) {
        if (world == null) {
            return false;
        }
        double fleeRangeSq = species.getFleePlayerRange() * species.getFleePlayerRange();
        boolean[] fled = {false};
        commandBuffer
            .getStore()
            .forEachEntityParallel(
                Player.getComponentType(),
                (idx, chunk, ignored) -> {
                    if (fled[0]) {
                        return;
                    }
                    TransformComponent playerTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
                    if (playerTransform == null) {
                        return;
                    }
                    Vector3d playerPos = playerTransform.getPosition();
                    if (!FishShadowSpawnHelper.isInWater(world, playerPos)) {
                        return;
                    }
                    double dx = playerPos.x - position.x;
                    double dz = playerPos.z - position.z;
                    if (dx * dx + dz * dz > fleeRangeSq) {
                        return;
                    }
                    shadow.setWanderDirection(FishShadowVision.yawToward(playerPos, position));
                    shadow.setState(FishShadowState.FLEEING);
                    shadow.setFleeTimer(0.0f);
                    fled[0] = true;
                }
            );
        return fled[0];
    }

    @Nullable
    private static Ref<EntityStore> resolveTargetBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Vector3d position,
        float searchRange
    ) {
        Ref<EntityStore> stored = shadow.getTargetBobberRef();
        if (isActiveFloatingBobber(commandBuffer, stored)) {
            return stored;
        }

        UUID ownerUuid = shadow.getTargetBobberOwnerUuid();
        if (ownerUuid != null) {
            Ref<EntityStore> byOwner = FishingLineService.findBobberForOwner(commandBuffer, ownerUuid);
            if (isActiveFloatingBobber(commandBuffer, byOwner)) {
                shadow.setTargetBobberRef(byOwner);
                return byOwner;
            }
        }

        Ref<EntityStore> nearest = findNearestFloatingBobber(commandBuffer, position, searchRange);
        if (nearest != null) {
            assignTargetBobber(commandBuffer, shadow, nearest);
        }
        return nearest;
    }

    private static void assignTargetBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Ref<EntityStore> bobberRef
    ) {
        shadow.setTargetBobberRef(bobberRef);
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber != null) {
            shadow.setTargetBobberOwnerUuid(bobber.getOwnerUuid());
        }
    }

    private static void clearBobberTarget(@Nonnull FishShadowComponent shadow) {
        shadow.setTargetBobberRef(null);
        shadow.setTargetBobberOwnerUuid(null);
        shadow.clearBobberAnchor();
        shadow.setPokePhase(FishShadowPokePhase.LURK);
    }

    private static boolean sanitizeBrokenShadow(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow
    ) {
        FishShadowState state = shadow.getState();
        if (state == FishShadowState.WANDERING || state == FishShadowState.FLEEING || state == FishShadowState.DESPAWNING) {
            return false;
        }
        if (state == FishShadowState.FIGHTING) {
            UUID ownerUuid = shadow.getHookedPlayerUuid();
            if (ownerUuid == null || findPlayerRef(ownerUuid) == null) {
                FishShadowEntityPool.despawn(commandBuffer, shadowRef);
                return true;
            }
        }
        if (state == FishShadowState.POKING || state == FishShadowState.ALERT || state == FishShadowState.HOOKED) {
            Ref<EntityStore> bobberRef = shadow.getTargetBobberRef();
            if (bobberRef == null && shadow.getTargetBobberOwnerUuid() == null) {
                shadow.setState(FishShadowState.WANDERING);
                clearBobberTarget(shadow);
            }
        }
        return false;
    }

    private static boolean isOwnerReelingDuringPoke(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Ref<EntityStore> bobberRef
    ) {
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null || bobber.isSubmerged() || bobber.getPhase() != FishingBobberPhase.FLOATING) {
            return false;
        }

        UUID ownerUuid = shadow.getTargetBobberOwnerUuid();
        if (ownerUuid == null) {
            ownerUuid = bobber.getOwnerUuid();
        }
        Ref<EntityStore> ownerEntity = findPlayerRef(ownerUuid);
        if (ownerEntity == null) {
            return false;
        }

        FishingLineComponent line = commandBuffer.getComponent(ownerEntity, FishingLineComponent.getComponentType());
        if (line == null || !line.isReeling()) {
            return false;
        }

        FishingLinePhase phase = line.getPhase();
        return phase != FishingLinePhase.FIGHTING && phase != FishingLinePhase.INACTIVE;
    }

    private static void startScaredFleeFromBobber(
        @Nonnull FishShadowComponent shadow,
        @Nonnull Vector3d position,
        @Nonnull Vector3d bobberPos
    ) {
        shadow.setWanderDirection(FishShadowVision.yawToward(bobberPos, position));
        shadow.setState(FishShadowState.FLEEING);
        shadow.setFleeTimer(0.0f);
        clearBobberTarget(shadow);
    }

    private static void startIdleDespawnFlee(@Nonnull FishShadowComponent shadow) {
        shadow.setWanderDirection((float) (Math.random() * Math.PI * 2.0));
        shadow.setState(FishShadowState.FLEEING);
        shadow.setFleeTimer(0.0f);
        clearBobberTarget(shadow);
    }

    private static float yawWhileSwimming(@Nonnull Vector3d position, @Nonnull Vector3d moveTarget) {
        double dx = moveTarget.x - position.x;
        double dz = moveTarget.z - position.z;
        if (dx * dx + dz * dz < 1.0e-8) {
            return 0.0f;
        }
        return FishShadowVision.swimYaw((float) dx, (float) dz);
    }

    @Nonnull
    private static Vector3d lurkPositionAround(
        @Nonnull Vector3d bobberPos,
        @Nonnull Vector3d shadowPos,
        float lurkDistance
    ) {
        double dx = shadowPos.x - bobberPos.x;
        double dz = shadowPos.z - bobberPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0e-4) {
            dx = 1.0;
            dz = 0.0;
            dist = 1.0;
        }
        double scale = lurkDistance / dist;
        return new Vector3d(bobberPos.x + dx * scale, shadowPos.y, bobberPos.z + dz * scale);
    }

    private static boolean swimAwayFromTarget(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull Vector3d target,
        float speedBlocksPerSecond,
        float dt
    ) {
        Vector3d awayTarget = new Vector3d(position.x * 2.0 - target.x, position.y, position.z * 2.0 - target.z);
        return swimTowardTarget(world, position, awayTarget, speedBlocksPerSecond, dt);
    }

    private static boolean isActiveFloatingBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nullable Ref<EntityStore> bobberRef
    ) {
        if (bobberRef == null || !bobberRef.isValid()) {
            return false;
        }
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        return bobber != null && bobber.getPhase() == FishingBobberPhase.FLOATING && !bobber.isSubmerged();
    }

    @Nullable
    private static Ref<EntityStore> findNearestFloatingBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d position,
        float searchRange
    ) {
        double rangeSq = searchRange * searchRange;
        AtomicReference<Ref<EntityStore>> nearest = new AtomicReference<>();
        double[] nearestDistSq = {Double.MAX_VALUE};
        commandBuffer.getStore().forEachEntityParallel(
            FishingBobberComponent.getComponentType(),
            (idx, chunk, ignored) -> {
                FishingBobberComponent bobber = chunk.getComponent(idx, FishingBobberComponent.getComponentType());
                TransformComponent bobberTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
                if (bobber == null || bobberTransform == null || bobber.getPhase() != FishingBobberPhase.FLOATING || bobber.isSubmerged()) {
                    return;
                }
                Vector3d bobberPos = bobberTransform.getPosition();
                double dx = bobberPos.x - position.x;
                double dz = bobberPos.z - position.z;
                double distSq = dx * dx + dz * dz;
                if (distSq <= rangeSq && distSq < nearestDistSq[0]) {
                    nearestDistSq[0] = distSq;
                    nearest.set(chunk.getReferenceTo(idx));
                }
            }
        );
        return nearest.get();
    }

    @Nullable
    private static Ref<EntityStore> findBobberInVision(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nonnull Vector3d position
    ) {
        AtomicReference<Ref<EntityStore>> holder = new AtomicReference<>();
        commandBuffer.getStore().forEachEntityParallel(
            FishingBobberComponent.getComponentType(),
            (idx, chunk, ignored) -> {
                if (holder.get() != null) {
                    return;
                }
                FishingBobberComponent bobber = chunk.getComponent(idx, FishingBobberComponent.getComponentType());
                TransformComponent bobberTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
                if (bobber == null || bobberTransform == null || bobber.getPhase() != FishingBobberPhase.FLOATING || bobber.isSubmerged()) {
                    return;
                }
                Vector3d bobberPos = bobberTransform.getPosition();
                if (
                    FishShadowVision.isInHorizontalCone(
                        position,
                        shadow.getWanderDirection(),
                        bobberPos,
                        species.getVisionRange(),
                        species.getVisionAngleDegrees() * 0.5f
                    )
                ) {
                    holder.set(chunk.getReferenceTo(idx));
                }
            }
        );
        return holder.get();
    }

    @Nonnull
    private static Vector3d getBobberPosition(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nullable Ref<EntityStore> bobberRef
    ) {
        if (bobberRef == null || !bobberRef.isValid()) {
            return new Vector3d();
        }
        TransformComponent transform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        return transform != null ? new Vector3d(transform.getPosition()) : new Vector3d();
    }

    /** When a straight approach is blocked, try slight left/right strafe. */
    private static void tryStrafeApproachMove(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull Vector3d approachDir,
        float step
    ) {
        double perpX = -approachDir.z;
        double perpZ = approachDir.x;
        for (double sign : new double[] {1.0, -1.0, 0.65, -0.65}) {
            double moveX = (approachDir.x + perpX * sign) * step;
            double moveZ = (approachDir.z + perpZ * sign) * step;
            if (FishShadowSpawnHelper.tryMoveOnWaterSurface(world, position, moveX, moveZ)) {
                return;
            }
        }
    }

    /** When a straight flee is blocked, try slight left/right strafe instead of snapping sideways. */
    private static void tryStrafeFightMove(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull Vector3d fleeDir,
        float step
    ) {
        double perpX = -fleeDir.z;
        double perpZ = fleeDir.x;
        for (double sign : new double[] {1.0, -1.0, 0.65, -0.65}) {
            double moveX = (fleeDir.x + perpX * sign) * step;
            double moveZ = (fleeDir.z + perpZ * sign) * step;
            if (FishShadowSpawnHelper.tryMoveOnWaterSurface(world, position, moveX, moveZ)) {
                return;
            }
        }
    }

    private static boolean swimTowardTarget(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull Vector3d target,
        float speedBlocksPerSecond,
        float dt
    ) {
        double startX = position.x;
        double startZ = position.z;
        Vector3d delta = new Vector3d(target.x - position.x, 0.0, target.z - position.z);
        double distance = delta.length();
        if (distance < 1.0e-6) {
            snapShadowToWaterSurface(world, position);
            return false;
        }
        float step = speedBlocksPerSecond * dt;
        if (step >= distance) {
            FishShadowSpawnHelper.tryMoveOnWaterSurface(world, position, target.x - position.x, target.z - position.z);
        } else {
            delta.normalize();
            if (!FishShadowSpawnHelper.tryMoveOnWaterSurface(world, position, delta.x * step, delta.z * step)) {
                tryStrafeApproachMove(world, position, delta, step);
            }
        }
        snapShadowToWaterSurface(world, position);
        return startX != position.x || startZ != position.z;
    }

    @Nullable
    private static Ref<EntityStore> findPlayerRef(@Nullable UUID ownerUuid) {
        if (ownerUuid == null) {
            return null;
        }
        PlayerRef playerRef = Universe.get().getPlayer(ownerUuid);
        return playerRef != null ? playerRef.getReference() : null;
    }

    private static void snapShadowToWaterSurface(@Nullable World world, @Nonnull Vector3d position) {
        FishShadowSpawnHelper.snapShadowToWaterSurface(world, position);
    }
}
