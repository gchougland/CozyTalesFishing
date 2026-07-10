package com.hexvane.cozytalefishing.fishfinder;

import com.hexvane.cozytalefishing.fish.FishFluidHelper;
import com.hexvane.cozytalefishing.fish.SpawnProbeService;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Opens the Fish Finder UI for the water/lava block the player is targeting. */
public final class OpenFishFinderInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<OpenFishFinderInteraction> CODEC =
        BuilderCodec.builder(OpenFishFinderInteraction.class, OpenFishFinderInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Opens the Fish Finder UI for the targeted fishable fluid block.")
            .build();

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Client;
    }

    @Override
    public boolean needsRemoteSync() {
        return true;
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        if (player.getPageManager().getCustomPage() != null) {
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        Vector3i target = resolveFluidTarget(world, ref, commandBuffer, context);
        SpawnProbeService.PlayerProbeResult location = null;
        if (target != null) {
            location = SpawnProbeService.probeAtBlock(world, target.x, target.y, target.z);
        }

        player.getPageManager().openCustomPage(ref, commandBuffer.getStore(), new FishFinderPage(playerRef, location));
        context.getState().state = InteractionState.Finished;
    }

    @Nullable
    private static Vector3i resolveFluidTarget(
        @Nonnull World world,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context
    ) {
        var clientState = context.getClientState();
        if (clientState != null && clientState.blockPosition != null) {
            BlockPosition pos = clientState.blockPosition;
            context.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK, pos);
            if (FishFluidHelper.isFishableFluidAt(world, pos.x, pos.y, pos.z)
                || FishFluidHelper.isFishableFluidAt(world, pos.x, pos.y - 1, pos.z)) {
                int y = FishFluidHelper.isFishableFluidAt(world, pos.x, pos.y, pos.z) ? pos.y : pos.y - 1;
                return new Vector3i(pos.x, y, pos.z);
            }
        }

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock != null) {
            if (FishFluidHelper.isFishableFluidAt(world, targetBlock.x, targetBlock.y, targetBlock.z)) {
                return new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
            }
            if (FishFluidHelper.isFishableFluidAt(world, targetBlock.x, targetBlock.y - 1, targetBlock.z)) {
                return new Vector3i(targetBlock.x, targetBlock.y - 1, targetBlock.z);
            }
        }

        Vector3i raycast = FishFluidHelper.raycastFishableFluidTarget(world, ref, commandBuffer);
        if (raycast == null) {
            return null;
        }
        if (FishFluidHelper.isFishableFluidAt(world, raycast.x, raycast.y, raycast.z)) {
            return raycast;
        }
        // Ray hit a solid block; try the fluid cell just beyond / below the hit.
        if (FishFluidHelper.isFishableFluidAt(world, raycast.x, raycast.y - 1, raycast.z)) {
            return new Vector3i(raycast.x, raycast.y - 1, raycast.z);
        }
        return null;
    }
}
