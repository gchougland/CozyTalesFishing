package com.hexvane.cozytalefishing.bench;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Opens the Fishing Bench crafting UI with sorted recipe lists. */
public final class OpenFishingBenchInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<OpenFishingBenchInteraction> CODEC =
        BuilderCodec.builder(OpenFishingBenchInteraction.class, OpenFishingBenchInteraction::new, SimpleBlockInteraction.CODEC)
            .documentation("Opens the Fishing Bench crafting page with ordered recipes.")
            .build();

    public OpenFishingBenchInteraction() {}

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
        var ref = context.getEntity();
        var store = ref.getStore();

        var playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        var craftingManagerComponent = commandBuffer.getComponent(ref, CraftingManager.getComponentType());
        if (craftingManagerComponent == null || craftingManagerComponent.hasBenchSet()) {
            return;
        }

        var chunkStore = world.getChunkStore();
        var chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z));
        if (chunkRef == null || !chunkRef.isValid()) {
            return;
        }
        var blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) {
            return;
        }
        var blockEntityRef = blockComponentChunk.getEntityReference(
            ChunkUtil.indexBlockInColumn(targetBlock.x, targetBlock.y, targetBlock.z)
        );
        if (blockEntityRef == null || !blockEntityRef.isValid()) {
            return;
        }
        var benchBlock = chunkStore.getStore().getComponent(blockEntityRef, BenchBlock.getComponentType());
        if (benchBlock == null) {
            return;
        }

        BlockType blockType = world.getBlockType(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockType == null) {
            return;
        }
        var worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z));
        int rotationIndex = worldChunk.getRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);

        var benchWindow = new FishingBenchCraftingWindow(
            targetBlock.x,
            targetBlock.y,
            targetBlock.z,
            rotationIndex,
            blockType,
            benchBlock
        );

        var uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        var uuid = uuidComponent.getUuid();

        // Always replace so reopen uses a freshly sorted window (putIfAbsent can keep a stale one).
        var previous = benchBlock.getWindows().put(uuid, benchWindow);
        if (previous != null && previous != benchWindow) {
            previous.close(ref, commandBuffer);
        }
        benchWindow.registerCloseEvent(event -> benchBlock.getWindows().remove(uuid, benchWindow));

        playerComponent.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, benchWindow);
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
