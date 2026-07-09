package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

import static com.hypixel.hytale.server.core.modules.interaction.interaction.util.InteractionValidation.canPlayerInteractWithBlock;

/** Spawns a mountable fishing boat NPC on water (horse-style mount movement). */
public final class SpawnFishingBoatInteraction extends SimpleBlockInteraction {
  public static final BuilderCodec<SpawnFishingBoatInteraction> CODEC =
      BuilderCodec.builder(SpawnFishingBoatInteraction.class, SpawnFishingBoatInteraction::new, SimpleBlockInteraction.CODEC)
          .documentation("Spawns a mountable fishing boat NPC on water at the target block")
          .appendInherited(
              new KeyedCodec<>("Model", Codec.STRING),
              (o, v) -> o.modelId = v,
              o -> o.modelId,
              (o, p) -> o.modelId = p.modelId
          )
          .add()
          .build();

  private String modelId = FishingBoatConstants.MODEL_ID;

  @Override
  protected void tick0(
      boolean firstRun,
      float time,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nonnull CooldownHandler cooldownHandler
  ) {
    if (!firstRun) {
      return;
    }

    Ref<EntityStore> ref = context.getEntity();
    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
    if (commandBuffer == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    World world = commandBuffer.getExternalData().getWorld();
    BlockPosition targetBlockPos = resolveTargetBlock(world, ref, commandBuffer, context);
    if (targetBlockPos == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    if (!canPlayerInteractWithBlock(ref, commandBuffer, context.getHeldItem(), targetBlockPos)) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    Vector3i targetBlock = new Vector3i(targetBlockPos.x, targetBlockPos.y, targetBlockPos.z);
    var chunkStore = world.getChunkStore();
    long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
    var chunkReference = chunkStore.getChunkReference(chunkIndex);
    if (chunkReference == null || !chunkReference.isValid()) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    WorldChunk worldChunk = chunkStore.getStore().getComponent(chunkReference, WorldChunk.getComponentType());
    if (worldChunk == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    int blockId = worldChunk.getBlock(targetBlock);
    boolean isWaterCell = BoatWaterHelper.isWaterTargetBlock(world, targetBlock);
    if (blockId == BlockType.UNKNOWN_ID || (blockId == BlockType.EMPTY_ID && !isWaterCell)) {
      Vector3i waterTarget = BoatWaterHelper.raycastWaterTarget(world, ref, commandBuffer);
      if (waterTarget == null) {
        context.getState().state = InteractionState.Failed;
        super.tick0(firstRun, time, type, context, cooldownHandler);
        return;
      }
      targetBlock = waterTarget;
    }

    ItemStack itemInHand = InventoryComponent.getItemInHand(commandBuffer, ref);
    interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
    super.tick0(firstRun, time, type, context, cooldownHandler);
  }

  @Nullable
  private BlockPosition resolveTargetBlock(
      @Nonnull World world,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull InteractionContext context
  ) {
    var clientState = context.getClientState();
    if (clientState != null && clientState.blockPosition != null) {
      var latestBlockPos = clientState.blockPosition;
      context.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK, latestBlockPos);
      return latestBlockPos;
    }

    BlockPosition targetBlockPos = context.getTargetBlock();
    if (targetBlockPos != null) {
      return targetBlockPos;
    }

    Vector3i waterTarget = BoatWaterHelper.raycastWaterTarget(world, ref, commandBuffer);
    if (waterTarget == null) {
      return null;
    }
    var resolved = new BlockPosition(waterTarget.x, waterTarget.y, waterTarget.z);
    context.getMetaStore().putMetaObject(Interaction.TARGET_BLOCK, resolved);
    return resolved;
  }

  @Override
  protected void interactWithBlock(
      @Nonnull World world,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nullable ItemStack itemInHand,
      @Nonnull Vector3i targetBlock,
      @Nonnull CooldownHandler cooldownHandler
  ) {
    BoatWaterHelper.WaterPlacement placement =
        BoatWaterHelper.resolveWaterPlacement(world, targetBlock.x, targetBlock.y, targetBlock.z);
    if (!BoatWaterHelper.isValidPlacement(world, placement)) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    Rotation3f rotation = new Rotation3f();
    Ref<EntityStore> playerRef = context.getEntity();
    HeadRotation headRotation = commandBuffer.getComponent(playerRef, HeadRotation.getComponentType());
    if (headRotation != null) {
      rotation.setYaw(headRotation.getRotation().yaw());
    }

    String sourceItem =
        itemInHand != null && !itemInHand.isEmpty() ? itemInHand.getItemId() : FishingBoatComponent.DEFAULT_SOURCE_ITEM;

    Vector3d spawnPosition =
        new Vector3d(placement.blockX() + 0.5, placement.surfaceY(), placement.blockZ() + 0.5);

    commandBuffer.run(
        store -> {
          Ref<EntityStore> spawned =
              FishingBoatSpawner.spawnBoat(store, spawnPosition, rotation.yaw(), sourceItem);
          if (spawned == null) {
            context.getState().state = InteractionState.Failed;
            return;
          }
          consumeBoatItemIfNeeded(playerRef, context, commandBuffer, itemInHand);
        }
    );
  }

  private static void consumeBoatItemIfNeeded(
      @Nonnull Ref<EntityStore> playerRef,
      @Nonnull InteractionContext context,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nullable ItemStack itemInHand
  ) {
    if (itemInHand == null || itemInHand.isEmpty()) {
      return;
    }

    Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
    if (player == null || player.getGameMode() != GameMode.Adventure) {
      return;
    }

    var slotTransaction =
        context.getHeldItemContainer().removeItemStackFromSlot(context.getHeldItemSlot(), itemInHand, 1);
    if (slotTransaction.succeeded()) {
      context.setHeldItem(slotTransaction.getSlotAfter());
    }
  }

  @Override
  protected void simulateInteractWithBlock(
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nullable ItemStack itemInHand,
      @Nonnull World world,
      @Nonnull Vector3i targetBlock
  ) {}
}
