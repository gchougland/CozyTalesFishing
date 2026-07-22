package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.bobber.BobberEffects;
import com.hexvane.cozytalefishing.fishing.FishingBobberComponent;
import com.hexvane.cozytalefishing.fishing.FishingBobberOrientation;
import com.hexvane.cozytalefishing.fishing.FishingBobberPhase;
import com.hexvane.cozytalefishing.fishing.FishingConstants;
import com.hexvane.cozytalefishing.fishing.FishingFightHudService;
import com.hexvane.cozytalefishing.fishing.FishingLineComponent;
import com.hexvane.cozytalefishing.fishing.FishingLinePhase;
import com.hexvane.cozytalefishing.fishing.FishingLineService;
import com.hexvane.cozytalefishing.fishing.FishingRodRegistry;
import com.hexvane.cozytalefishing.fishing.FishingStaminaResolver;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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
                    startIdleDespawnFlee(commandBuffer, shadowRef, shadow);
                    tickFleeing(commandBuffer, shadowRef, shadow, species, world, position, dt);
                    return;
                }
            }
        }

        Ref<EntityStore> bobberRef = findBobberInVision(commandBuffer, shadowRef, shadow, species, position);
        if (bobberRef != null && beginPokeSequence(commandBuffer, shadowRef, shadow, bobberRef)) {
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
        if (!FishShadowSpawnHelper.tryMoveOnFluidSurface(world, position, Math.sin(shadow.getWanderDirection()) * speed, Math.cos(shadow.getWanderDirection()) * speed, FishFluidHelper.columnFluidForShadow(shadow))) {
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
        Ref<EntityStore> bobberRef = resolveTargetBobber(commandBuffer, shadowRef, shadow, position, species.getVisionRange() * 1.5f);
        if (bobberRef == null) {
            clearBobberTarget(commandBuffer, shadowRef, shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
        if (!shadow.isBobberAnchorInitialized()) {
            shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
        }

        if (isOwnerReelingDuringPoke(commandBuffer, shadow, bobberRef)) {
            startScaredFleeFromBobber(commandBuffer, shadowRef, shadow, position, bobberPos);
            if (!FishShadowSpawnHelper.tryMoveOnFluidSurface(
                world,
                position,
                Math.sin(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.4f * dt,
                Math.cos(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.4f * dt,
                FishFluidHelper.columnFluidForShadow(shadow)
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
                boolean moved = swimTowardTarget(world, position, lurkTarget, swimSpeed * 0.75f, dt, FishFluidHelper.columnFluidForShadow(shadow));
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
                boolean moved = swimTowardTarget(world, position, bobberPos, swimSpeed * POKE_LUNGE_SPEED_FACTOR, dt, FishFluidHelper.columnFluidForShadow(shadow));
                dx = bobberPos.x - position.x;
                dz = bobberPos.z - position.z;
                dist = Math.sqrt(dx * dx + dz * dz);
                yaw = FishShadowVision.swimYaw((float) dx, (float) dz);
                if (dist <= pokeReach || (!moved && dist <= lurkDistance * 1.2f)) {
                    FishShadowEffects.playPoke(commandBuffer, bobberPos, shadow.getWaterBodyType());
                    shadow.setBobberAnchor(bobberPos.x, bobberPos.z);
                    shadow.setPokePhase(FishShadowPokePhase.RETREAT);
                    shadow.setStateTimer(0.0f);
                }
            }
            case RETREAT -> {
                double distBefore = dist;
                boolean moved = swimAwayFromTarget(world, position, bobberPos, swimSpeed * POKE_RETREAT_SPEED_FACTOR, dt, FishFluidHelper.columnFluidForShadow(shadow));
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

    private static boolean beginPokeSequence(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Ref<EntityStore> bobberRef
    ) {
        if (!tryClaimBobberForPoking(commandBuffer, shadowRef, bobberRef)) {
            return false;
        }
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
        return true;
    }

    private static void commitHookPullUnder(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species
    ) {
        Ref<EntityStore> bobberRef = resolveHookBobber(commandBuffer, shadow);
        if (bobberRef == null) {
            clearBobberTarget(commandBuffer, shadowRef, shadow);
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
            clearBobberTarget(commandBuffer, shadowRef, shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null || bobber.getPhase() != FishingBobberPhase.FLOATING) {
            clearBobberTarget(commandBuffer, shadowRef, shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);

        double dx = bobberPos.x - position.x;
        double dz = bobberPos.z - position.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float pokeReach = species.getPokeReachBlocks();
        boolean moved = swimTowardTarget(world, position, bobberPos, species.getSwimSpeed() * HOOK_APPROACH_SPEED_FACTOR, dt, FishFluidHelper.columnFluidForShadow(shadow));
        dx = bobberPos.x - position.x;
        dz = bobberPos.z - position.z;
        dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = FishShadowVision.swimYaw((float) dx, (float) dz);
        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, yaw, shadow.getCurrentScale());

        if (dist <= pokeReach) {
            beginFight(commandBuffer, shadowRef, shadow, species, position);
        } else if (!moved) {
            shadow.setPokeTimer(shadow.getPokeTimer() + dt);
            if (shadow.getPokeTimer() >= POKE_RETREAT_STUCK_SECONDS || dist <= POKE_LURK_DISTANCE * 1.2f) {
                beginFight(commandBuffer, shadowRef, shadow, species, position);
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
        double submergedY = bobber != null
            ? bobber.getLatchedSurfaceY() - FishingConstants.FIGHT_BOBBER_SUBMERGE_OFFSET
            : bobberTransform.getPosition().y - FishingConstants.FIGHT_BOBBER_SUBMERGE_OFFSET;
        bobberTransform.setPosition(new Vector3d(anchorX, submergedY, anchorZ));
        FishingBobberOrientation.applyUpright(commandBuffer, bobberRef);
    }

    private static void beginFight(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nonnull Vector3d position
    ) {
        Ref<EntityStore> bobberRef = resolveHookBobber(commandBuffer, shadow);
        if (bobberRef == null) {
            clearBobberTarget(commandBuffer, shadowRef, shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null) {
            clearBobberTarget(commandBuffer, shadowRef, shadow);
            shadow.setState(FishShadowState.WANDERING);
            return;
        }

        Vector3d bobberPos = getBobberPosition(commandBuffer, bobberRef);
        if (!bobber.isSubmerged()) {
            bobber.setSubmerged(true);
            FishShadowEffects.playPullUnder(commandBuffer, bobberPos, shadow.getWaterBodyType());
        }
        bobber.setHookedShadowRef(shadowRef);
        bobber.setPokingShadowRef(null);
        UUID ownerUuid = bobber.getOwnerUuid();
        shadow.setHookedPlayerUuid(ownerUuid);
        PlayerRef playerRef = Universe.get().getPlayer(ownerUuid);
        Ref<EntityStore> ownerEntity = null;
        if (playerRef != null) {
            ownerEntity = playerRef.getReference();
            if (ownerEntity != null && ownerEntity.isValid()) {
                FishingLineComponent line = commandBuffer.getComponent(ownerEntity, FishingLineComponent.getComponentType());
                if (line != null) {
                    FishingModConfig config = FishingModConfig.get();
                    FishingRodRegistry.FishingRodStats rodStats =
                        FishingStaminaResolver.resolveRodStats(commandBuffer, ownerEntity, config);
                    float difficulty = species.getFightDifficulty(config);
                    ItemStack heldRod = InventoryComponent.getItemInHand(commandBuffer, ownerEntity);
                    BobberEffects bobberEffects =
                        heldRod != null && !ItemStack.isEmpty(heldRod) ? BobberEffects.fromRod(heldRod) : BobberEffects.NONE;
                    line.setPhase(FishingLinePhase.FIGHTING);
                    line.setHookedShadowRef(shadowRef);
                    line.setRolledSizeCm(
                        species.excludesFromJournal()
                            ? 0.0f
                            : FishCatchService.rollSizeCm(species, bobberEffects.getSizeSkewExponent())
                    );
                    line.setFightStartMaxLength(line.getMaxLength());
                    line.setReeledDuringFightBlocks(0.0f);
                    line.setFishingStaminaMax(rodStats.maxStamina());
                    line.setFishingStamina(rodStats.maxStamina());
                    line.setFishingStaminaRegenPerSecond(rodStats.regenPerSecond());
                    line.setFightDifficulty(difficulty);
                    commandBuffer.putComponent(ownerEntity, FishingLineComponent.getComponentType(), line);

                    Player player = commandBuffer.getComponent(ownerEntity, Player.getComponentType());
                    if (player != null) {
                        FishingFightHudService.show(player, playerRef, rodStats.maxStamina());
                    }
                }
            }
        }
        commandBuffer.putComponent(bobberRef, FishingBobberComponent.getComponentType(), bobber);

        pinBobberSubmerged(commandBuffer, shadow, bobberRef, bobber);
        if (ownerEntity != null && ownerEntity.isValid()) {
            TransformComponent ownerTransform = commandBuffer.getComponent(ownerEntity, TransformComponent.getComponentType());
            if (ownerTransform != null) {
                Vector3d ownerPos = ownerTransform.getPosition();
                double dx = position.x - ownerPos.x;
                double dz = position.z - ownerPos.z;
                shadow.setFightStartPlayerDistance((float) Math.sqrt(dx * dx + dz * dz));
            }
        }
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
        boolean reeling = line != null && line.isFightReelingActive();
        FishingModConfig config = FishingModConfig.get();
        float difficulty = line != null ? Math.max(0.1f, line.getFightDifficulty()) : 1.0f;

        float fleeSpeed = species.getFightSwimSpeed() * difficulty;
        ItemStack heldRod = InventoryComponent.getItemInHand(commandBuffer, ownerEntity);
        BobberEffects bobberEffects =
            heldRod != null && !ItemStack.isEmpty(heldRod) ? BobberEffects.fromRod(heldRod) : BobberEffects.NONE;
        float pullSpeed = config.getFightReelSpeedBlocksPerSecond() * bobberEffects.getReelSpeedMultiplier();
        float resist = fleeSpeed * config.getFightReelResistanceFactor();
        float counterPull = fleeSpeed * config.getFightReelCounterPullFactor() * difficulty;

        Vector3d moveDir = new Vector3d();
        float moveSpeed;
        if (reeling) {
            moveDir.set(ownerPos.x - position.x, 0.0, ownerPos.z - position.z);
            moveSpeed = Math.max(0.85f, pullSpeed - resist - counterPull);
        } else {
            moveDir.set(position.x - ownerPos.x, 0.0, position.z - ownerPos.z);
            moveSpeed = fleeSpeed * bobberEffects.getFleeSpeedMultiplier();
        }

        float yaw = shadow.getWanderDirection();
        if (moveDir.lengthSquared() > 1.0e-6) {
            moveDir.normalize();
            double step = moveSpeed * dt;
            if (!FishShadowSpawnHelper.tryMoveOnFluidSurface(world, position, moveDir.x * step, moveDir.z * step, FishFluidHelper.columnFluidForShadow(shadow))) {
                tryStrafeFightMove(world, position, moveDir, (float) step, FishFluidHelper.columnFluidForShadow(shadow));
            }
            yaw = FishShadowVision.swimYaw((float) moveDir.x, (float) moveDir.z);
        } else {
            snapShadowToFluidSurface(world, position, shadow);
        }

        FishShadowEntityPool.updateTransform(commandBuffer, shadowRef, position, yaw, shadow.getCurrentScale());

        if (shadow.getFightStartPlayerDistance() >= 0.0f) {
            double dx = position.x - ownerPos.x;
            double dz = position.z - ownerPos.z;
            double currentPlayerDistance = Math.sqrt(dx * dx + dz * dz);
            double extraRun = currentPlayerDistance - shadow.getFightStartPlayerDistance();
            if (extraRun > species.getFightEscapeDistanceBlocks(config)) {
                triggerFightEscape(commandBuffer, shadowRef, shadow, species, world, ownerEntity, ownerPos, position);
                return;
            }
        }

        if (reeling) {
            shadow.setStateTimer(shadow.getStateTimer() - dt);
            if (shadow.getStateTimer() <= 0.0f) {
                FishShadowEffects.playFightSplash(commandBuffer, position, shadow.getWaterBodyType());
                shadow.setStateTimer(FIGHT_SPLASH_INTERVAL);
            }
        }

        Ref<EntityStore> bobberRef = shadow.getTargetBobberRef();
        if (bobberRef != null && bobberRef.isValid()) {
            TransformComponent bobberTransform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
            FishingBobberComponent bobberState = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
            if (bobberTransform != null) {
                int surfaceBlockY =
                    FishShadowSpawnHelper.findSurfaceFluidBlockYNear(
                        world,
                        (int) Math.floor(position.x),
                        (int) Math.floor(position.z),
                        position.y,
                        FishFluidHelper.columnFluidForShadow(shadow)
                    );
                double surfaceWorldY =
                    surfaceBlockY >= 0 ? FishShadowSpawnHelper.waterSurfaceWorldY(surfaceBlockY) : position.y;
                double submergedY = surfaceWorldY - FishingConstants.FIGHT_BOBBER_SUBMERGE_OFFSET;
                if (bobberState != null && bobberState.getPhase() == FishingBobberPhase.FLOATING) {
                    submergedY = bobberState.getLatchedSurfaceY() - FishingConstants.FIGHT_BOBBER_SUBMERGE_OFFSET;
                }
                Vector3d bobberPos = bobberTransform.getPosition();
                float followLerp = Math.min(1.0f, 5.0f * dt);
                double newX = bobberPos.x + (position.x - bobberPos.x) * followLerp;
                double newZ = bobberPos.z + (position.z - bobberPos.z) * followLerp;
                bobberTransform.setPosition(new Vector3d(newX, submergedY, newZ));
                FishingBobberOrientation.applyUpright(commandBuffer, bobberRef);
            }
        }
    }

    private static void triggerFightEscape(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull FishSpeciesAsset species,
        @Nullable World world,
        @Nonnull Ref<EntityStore> ownerEntity,
        @Nonnull Vector3d ownerPos,
        @Nonnull Vector3d position
    ) {
        FishCatchService.notifyEscape(commandBuffer, ownerEntity, species);
        FishShadowEffects.playFightSplash(commandBuffer, position, shadow.getWaterBodyType());

        shadow.setWanderDirection(FishShadowVision.yawToward(ownerPos, position));
        shadow.setHookedPlayerUuid(null);
        clearBobberTarget(commandBuffer, shadowRef, shadow);
        shadow.setState(FishShadowState.FLEEING);
        shadow.setFleeTimer(0.0f);

        FishingLineComponent line = commandBuffer.getComponent(ownerEntity, FishingLineComponent.getComponentType());
        if (line != null) {
            line.setHookedShadowRef(null);
            line.setReeling(false);
            commandBuffer.putComponent(ownerEntity, FishingLineComponent.getComponentType(), line);
        }

        Player player = commandBuffer.getComponent(ownerEntity, Player.getComponentType());
        PlayerRef playerRefComponent = commandBuffer.getComponent(ownerEntity, PlayerRef.getComponentType());
        FishingFightHudService.hide(player, playerRefComponent);

        FishingLineService.recallCastOut(commandBuffer, ownerEntity);
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
        if (!FishShadowSpawnHelper.tryMoveOnFluidSurface(
            world,
            position,
            Math.sin(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.5f * dt,
            Math.cos(shadow.getWanderDirection()) * species.getSwimSpeed() * 1.5f * dt,
            FishFluidHelper.columnFluidForShadow(shadow)
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
        @Nonnull Ref<EntityStore> shadowRef,
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

        Ref<EntityStore> nearest = findNearestFloatingBobber(commandBuffer, shadowRef, position, searchRange);
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

    private static void clearBobberTarget(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow
    ) {
        releaseBobberPokingClaim(commandBuffer, shadowRef, shadow.getTargetBobberRef());
        shadow.setTargetBobberRef(null);
        shadow.setTargetBobberOwnerUuid(null);
        shadow.clearBobberAnchor();
        shadow.clearFightStartPlayerDistance();
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
                clearBobberTarget(commandBuffer, shadowRef, shadow);
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
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow,
        @Nonnull Vector3d position,
        @Nonnull Vector3d bobberPos
    ) {
        shadow.setWanderDirection(FishShadowVision.yawToward(bobberPos, position));
        shadow.setState(FishShadowState.FLEEING);
        shadow.setFleeTimer(0.0f);
        clearBobberTarget(commandBuffer, shadowRef, shadow);
    }

    private static void startIdleDespawnFlee(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull FishShadowComponent shadow
    ) {
        shadow.setWanderDirection((float) (Math.random() * Math.PI * 2.0));
        shadow.setState(FishShadowState.FLEEING);
        shadow.setFleeTimer(0.0f);
        clearBobberTarget(commandBuffer, shadowRef, shadow);
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
        float dt,
        @Nonnull FishFluidHelper.ColumnFluid fluid
    ) {
        Vector3d awayTarget = new Vector3d(position.x * 2.0 - target.x, position.y, position.z * 2.0 - target.z);
        return swimTowardTarget(world, position, awayTarget, speedBlocksPerSecond, dt, fluid);
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

    private static boolean isBobberAvailableForShadow(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nullable Ref<EntityStore> requestingShadowRef
    ) {
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null) {
            return false;
        }
        Ref<EntityStore> poking = bobber.getPokingShadowRef();
        if (poking != null && poking.isValid() && (requestingShadowRef == null || !poking.equals(requestingShadowRef))) {
            return false;
        }
        Ref<EntityStore> hooked = bobber.getHookedShadowRef();
        if (hooked != null && hooked.isValid() && (requestingShadowRef == null || !hooked.equals(requestingShadowRef))) {
            return false;
        }
        return true;
    }

    private static boolean tryClaimBobberForPoking(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull Ref<EntityStore> bobberRef
    ) {
        if (!isBobberAvailableForShadow(commandBuffer, bobberRef, shadowRef)) {
            return false;
        }
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null) {
            return false;
        }
        bobber.setPokingShadowRef(shadowRef);
        commandBuffer.putComponent(bobberRef, FishingBobberComponent.getComponentType(), bobber);
        return true;
    }

    private static void releaseBobberPokingClaim(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nullable Ref<EntityStore> bobberRef
    ) {
        if (bobberRef == null || !bobberRef.isValid()) {
            return;
        }
        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, FishingBobberComponent.getComponentType());
        if (bobber == null) {
            return;
        }
        Ref<EntityStore> claim = bobber.getPokingShadowRef();
        if (claim != null && claim.equals(shadowRef)) {
            bobber.setPokingShadowRef(null);
            commandBuffer.putComponent(bobberRef, FishingBobberComponent.getComponentType(), bobber);
        }
    }

    @Nullable
    private static Ref<EntityStore> findNearestFloatingBobber(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nullable Ref<EntityStore> requestingShadowRef,
        @Nonnull Vector3d position,
        float searchRange
    ) {
        double rangeSq = searchRange * searchRange;
        AtomicReference<Ref<EntityStore>> nearest = new AtomicReference<>();
        double[] nearestDistSq = {Double.MAX_VALUE};
        commandBuffer.getStore().forEachEntityParallel(
            FishingBobberComponent.getComponentType(),
            (idx, chunk, ignored) -> {
                Ref<EntityStore> bobberRef = chunk.getReferenceTo(idx);
                FishingBobberComponent bobber = chunk.getComponent(idx, FishingBobberComponent.getComponentType());
                TransformComponent bobberTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
                if (bobber == null || bobberTransform == null || bobber.getPhase() != FishingBobberPhase.FLOATING || bobber.isSubmerged()) {
                    return;
                }
                if (!isBobberAvailableForShadow(commandBuffer, bobberRef, requestingShadowRef)) {
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
        @Nonnull Ref<EntityStore> shadowRef,
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
                Ref<EntityStore> bobberRef = chunk.getReferenceTo(idx);
                FishingBobberComponent bobber = chunk.getComponent(idx, FishingBobberComponent.getComponentType());
                TransformComponent bobberTransform = chunk.getComponent(idx, TransformComponent.getComponentType());
                if (bobber == null || bobberTransform == null || bobber.getPhase() != FishingBobberPhase.FLOATING || bobber.isSubmerged()) {
                    return;
                }
                if (!isBobberAvailableForShadow(commandBuffer, bobberRef, shadowRef)) {
                    return;
                }
                Vector3d bobberPos = bobberTransform.getPosition();
                float visionRange = resolveBobberVisionRange(commandBuffer, species, bobber);
                if (
                    FishShadowVision.isInHorizontalCone(
                        position,
                        shadow.getWanderDirection(),
                        bobberPos,
                        visionRange,
                        species.getVisionAngleDegrees() * 0.5f
                    )
                ) {
                    holder.set(chunk.getReferenceTo(idx));
                }
            }
        );
        return holder.get();
    }

    private static float resolveBobberVisionRange(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishSpeciesAsset species,
        @Nonnull FishingBobberComponent bobber
    ) {
        float multiplier = 1.0f;
        PlayerRef ownerPlayer = Universe.get().getPlayer(bobber.getOwnerUuid());
        if (ownerPlayer != null) {
            Ref<EntityStore> ownerRef = ownerPlayer.getReference();
            if (ownerRef != null && ownerRef.isValid()) {
                ItemStack heldRod = InventoryComponent.getItemInHand(commandBuffer, ownerRef);
                if (heldRod != null && !ItemStack.isEmpty(heldRod)) {
                    multiplier = BobberEffects.fromRod(heldRod).getVisionRangeMultiplier();
                }
            }
        }
        return species.getVisionRange() * multiplier;
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
        float step,
        @Nonnull FishFluidHelper.ColumnFluid fluid
    ) {
        double perpX = -approachDir.z;
        double perpZ = approachDir.x;
        for (double sign : new double[] {1.0, -1.0, 0.65, -0.65}) {
            double moveX = (approachDir.x + perpX * sign) * step;
            double moveZ = (approachDir.z + perpZ * sign) * step;
            if (FishShadowSpawnHelper.tryMoveOnFluidSurface(world, position, moveX, moveZ, fluid)) {
                return;
            }
        }
    }

    /** When a straight flee is blocked, try slight left/right strafe instead of snapping sideways. */
    private static void tryStrafeFightMove(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull Vector3d fleeDir,
        float step,
        @Nonnull FishFluidHelper.ColumnFluid fluid
    ) {
        double perpX = -fleeDir.z;
        double perpZ = fleeDir.x;
        for (double sign : new double[] {1.0, -1.0, 0.65, -0.65}) {
            double moveX = (fleeDir.x + perpX * sign) * step;
            double moveZ = (fleeDir.z + perpZ * sign) * step;
            if (FishShadowSpawnHelper.tryMoveOnFluidSurface(world, position, moveX, moveZ, fluid)) {
                return;
            }
        }
    }

    private static boolean swimTowardTarget(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull Vector3d target,
        float speedBlocksPerSecond,
        float dt,
        @Nonnull FishFluidHelper.ColumnFluid fluid
    ) {
        double startX = position.x;
        double startZ = position.z;
        Vector3d delta = new Vector3d(target.x - position.x, 0.0, target.z - position.z);
        double distance = delta.length();
        if (distance < 1.0e-6) {
            FishShadowSpawnHelper.snapShadowToFluidSurface(world, position, fluid);
            return false;
        }
        float step = speedBlocksPerSecond * dt;
        if (step >= distance) {
            FishShadowSpawnHelper.tryMoveOnFluidSurface(world, position, target.x - position.x, target.z - position.z, fluid);
        } else {
            delta.normalize();
            if (!FishShadowSpawnHelper.tryMoveOnFluidSurface(world, position, delta.x * step, delta.z * step, fluid)) {
                tryStrafeApproachMove(world, position, delta, step, fluid);
            }
        }
        FishShadowSpawnHelper.snapShadowToFluidSurface(world, position, fluid);
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

    private static void snapShadowToFluidSurface(
        @Nullable World world,
        @Nonnull Vector3d position,
        @Nonnull FishShadowComponent shadow
    ) {
        FishShadowSpawnHelper.snapShadowToFluidSurface(world, position, FishFluidHelper.columnFluidForShadow(shadow));
    }
}
