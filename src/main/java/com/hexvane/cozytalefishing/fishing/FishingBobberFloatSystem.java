package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Water landing, splash, bobbing, and ground fail handling for bobbers. */
public final class FishingBobberFloatSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Query<EntityStore> query =
        Query.and(FishingBobberComponent.getComponentType(), TransformComponent.getComponentType());

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
        Ref<EntityStore> bobberRef = archetypeChunk.getReferenceTo(index);
        FishingBobberComponent bobber = archetypeChunk.getComponent(index, FishingBobberComponent.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (bobber == null || transform == null) {
            return;
        }

        if (bobber.getPhase() == FishingBobberPhase.FLOATING) {
            FishingBobberOrientation.applyUpright(commandBuffer, bobberRef);
            if (bobber.isSubmerged()) {
                return;
            }
            bobber.setBobPhase(bobber.getBobPhase() + dt * FishingConstants.BOB_FREQUENCY);
            double y = bobber.getLatchedSurfaceY() + Math.sin(bobber.getBobPhase()) * FishingConstants.BOB_AMPLITUDE;
            Vector3d pos = transform.getPosition();
            transform.setPosition(new Vector3d(pos.x, y, pos.z));
            return;
        }

        if (bobber.getPhase() == FishingBobberPhase.GROUNDED) {
            bobber.setGroundedTimer(bobber.getGroundedTimer() + dt);
            if (bobber.getGroundedTimer() >= FishingConstants.GROUND_RECALL_DELAY_SECONDS) {
                recallOwner(commandBuffer, bobber.getOwnerUuid());
                bobber.setGroundedTimer(-1.0f);
            }
            return;
        }

        StandardPhysicsProvider physics = commandBuffer.getComponent(bobberRef, StandardPhysicsProvider.getComponentType());
        Vector3d pos = transform.getPosition();
        boolean inFluid = physics != null && physics.isInFluid();
        if (!inFluid) {
            inFluid = isFluidAt(store.getExternalData().getWorld(), pos);
        }

        if (inFluid) {
            latchOnWater(commandBuffer, bobberRef, bobber, transform, pos, physics);
            return;
        }

        if (physics != null && physics.getState() == StandardPhysicsProvider.STATE.RESTING) {
            bobber.setPhase(FishingBobberPhase.GROUNDED);
            bobber.setGroundedTimer(0.0f);
            stopPhysics(commandBuffer, bobberRef, physics);
        }
    }

    private static void latchOnWater(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nonnull FishingBobberComponent bobber,
        @Nonnull TransformComponent transform,
        @Nonnull Vector3d pos,
        @Nullable StandardPhysicsProvider physics
    ) {
        double surfaceY = Math.floor(pos.y) + 1.0;
        if (physics != null && physics.isInFluid()) {
            surfaceY = pos.y + 0.15;
        }

        bobber.setPhase(FishingBobberPhase.FLOATING);
        bobber.setLatchedSurfaceY(surfaceY);
        bobber.setBobPhase(0.0f);
        transform.setPosition(new Vector3d(pos.x, surfaceY, pos.z));
        FishingBobberOrientation.applyUpright(commandBuffer, bobberRef);

        if (!bobber.isSplashPlayed()) {
            bobber.setSplashPlayed(true);
            FishingSplashEffects.playBobberSplash(commandBuffer, new Vector3d(pos.x, surfaceY, pos.z));
        }

        stopPhysics(commandBuffer, bobberRef, physics);
    }

    private static void stopPhysics(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nullable StandardPhysicsProvider physics
    ) {
        Velocity velocity = commandBuffer.getComponent(bobberRef, Velocity.getComponentType());
        if (velocity != null) {
            velocity.setZero();
        }
        if (physics != null) {
            physics.getVelocity().zero();
            physics.setState(StandardPhysicsProvider.STATE.INACTIVE);
        }
    }

    private static boolean isFluidAt(@Nonnull World world, @Nonnull Vector3d pos) {
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);
        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(blockX, blockY, blockZ);
        if (sectionRef == null) {
            return false;
        }
        var fluidSection = chunkStore.getStore().getComponentConcurrent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return false;
        }
        return fluidSection.getFluidId(blockX, blockY, blockZ) != Fluid.EMPTY_ID;
    }

    private static void recallOwner(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UUID ownerUuid) {
        PlayerRef playerRef = Universe.get().getPlayer(ownerUuid);
        if (playerRef == null) {
            return;
        }
        Ref<EntityStore> ownerEntity = playerRef.getReference();
        if (ownerEntity == null || !ownerEntity.isValid()) {
            return;
        }
        FishingLineService.recallCastOut(commandBuffer, ownerEntity);
    }
}
