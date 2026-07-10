package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hexvane.cozytalefishing.fish.FishItemStackFactory;
import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Inserts or removes fish and decorations from aquarium blocks. */
public final class AquariumUseInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<AquariumUseInteraction> CODEC =
        BuilderCodec.builder(AquariumUseInteraction.class, AquariumUseInteraction::new, SimpleInteraction.CODEC)
            .documentation("Places a held fish or decoration into an aquarium, or removes contents with an empty hand")
            .build();

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
        Vector3i origin = AquariumBlockHelper.resolveOriginBlock(world, targetBlock);
        AquariumSize aquariumSize = AquariumBlockHelper.aquariumSizeAt(world, origin);
        if (aquariumSize == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<ChunkStore> blockRef = AquariumBlockHelper.blockEntityAtOrigin(world, origin);
        if (blockRef == null || !blockRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        AquariumBlock aquarium = chunkStore.getComponent(blockRef, AquariumBlock.getComponentType());
        if (aquarium == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> playerRef = context.getEntity();
        PlayerRef player = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());

        if (itemInHand == null || itemInHand.isEmpty()) {
            removeContents(context, commandBuffer, playerRef, player, chunkStore, blockRef, aquarium, origin, aquariumSize);
            return;
        }

        AquariumDecorationAsset decoration = AquariumDecorationRegistry.getByItemId(itemInHand.getItemId());
        if (decoration != null) {
            insertDecoration(
                context,
                commandBuffer,
                playerRef,
                player,
                chunkStore,
                blockRef,
                aquarium,
                origin,
                aquariumSize,
                itemInHand,
                decoration
            );
            return;
        }

        FishSpeciesAsset species = FishSpeciesRegistry.getSpeciesByItemId(itemInHand.getItemId());
        if (species != null) {
            insertFish(
                context,
                commandBuffer,
                playerRef,
                player,
                chunkStore,
                blockRef,
                aquarium,
                origin,
                aquariumSize,
                itemInHand,
                species
            );
            return;
        }

        send(player, "server.cozytalefishing.aquarium.not_a_decoration");
        context.getState().state = InteractionState.Failed;
    }

    private static void insertFish(
        @Nonnull InteractionContext context,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nullable PlayerRef player,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize aquariumSize,
        @Nonnull ItemStack itemInHand,
        @Nonnull FishSpeciesAsset species
    ) {
        if (aquarium.hasFish()) {
            send(player, "server.cozytalefishing.aquarium.already_occupied");
            context.getState().state = InteractionState.Failed;
            return;
        }

        AquariumSize requiredSize = species.getAquariumSize();
        if (requiredSize == null) {
            send(player, "server.cozytalefishing.aquarium.no_aquarium_size");
            context.getState().state = InteractionState.Failed;
            return;
        }

        if (!requiredSize.fitsIn(aquariumSize)) {
            send(player, "server.cozytalefishing.aquarium.fish_too_large");
            context.getState().state = InteractionState.Failed;
            return;
        }

        InventoryComponent.Hotbar hotbar = commandBuffer.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        byte slot = hotbar.getActiveSlot();
        ItemStackSlotTransaction removed = hotbar.getInventory().removeItemStackFromSlot(slot, 1);
        if (!removed.succeeded() || removed.getOutput() == null || removed.getOutput().isEmpty()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        aquarium.setFishItemId(removed.getOutput().getItemId());
        aquarium.setDisplayReference(null);
        chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);

        var world = chunkStore.getExternalData().getWorld();
        world.execute(
            () ->
                AquariumService.ensureDisplay(
                    world,
                    blockRef,
                    world.getChunkStore().getStore(),
                    origin,
                    aquariumSize
                )
        );

        send(player, "server.cozytalefishing.aquarium.fish_added");
        context.getState().state = InteractionState.Finished;
    }

    private static void insertDecoration(
        @Nonnull InteractionContext context,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nullable PlayerRef player,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize aquariumSize,
        @Nonnull ItemStack itemInHand,
        @Nonnull AquariumDecorationAsset decoration
    ) {
        AquariumLayoutAsset layout = AquariumLayoutRegistry.getLayout(aquariumSize);
        if (!aquarium.canAddDecoration(layout.getMaxDecorations())) {
            send(player, "server.cozytalefishing.aquarium.decorations_full");
            context.getState().state = InteractionState.Failed;
            return;
        }

        InventoryComponent.Hotbar hotbar = commandBuffer.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        byte slot = hotbar.getActiveSlot();
        ItemStackSlotTransaction removed = hotbar.getInventory().removeItemStackFromSlot(slot, 1);
        if (!removed.succeeded() || removed.getOutput() == null || removed.getOutput().isEmpty()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        String itemId = removed.getOutput().getItemId();
        aquarium.addDecoration(itemId);
        chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);

        int slotIndex = aquarium.getDecorationCount() - 1;
        AquariumService.spawnDecorationAtSlot(
            chunkStore.getExternalData().getWorld(),
            blockRef,
            chunkStore,
            origin,
            aquariumSize,
            aquarium,
            slotIndex
        );

        send(player, "server.cozytalefishing.aquarium.decoration_added");
        context.getState().state = InteractionState.Finished;
    }

    private static void removeContents(
        @Nonnull InteractionContext context,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nullable PlayerRef player,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize aquariumSize
    ) {
        if (aquarium.hasFish()) {
            removeFish(context, commandBuffer, playerRef, player, chunkStore, blockRef, aquarium, origin, aquariumSize);
            return;
        }

        if (aquarium.hasDecorations()) {
            removeLastDecoration(context, commandBuffer, playerRef, player, chunkStore, blockRef, aquarium, origin, aquariumSize);
            return;
        }

        send(player, "server.cozytalefishing.aquarium.empty");
        context.getState().state = InteractionState.Failed;
    }

    private static void removeFish(
        @Nonnull InteractionContext context,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nullable PlayerRef player,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize aquariumSize
    ) {
        String fishItemId = aquarium.getFishItemId();
        ItemStack fishStack = FishItemStackFactory.forItemId(fishItemId);

        var combinedInventory =
            InventoryComponent.getCombined(commandBuffer, playerRef, InventoryComponent.HOTBAR_FIRST);
        if (combinedInventory.canAddItemStack(fishStack)) {
            combinedInventory.addItemStack(fishStack);
        } else if (!Player.giveItem(fishStack, playerRef, commandBuffer).succeeded()) {
            var world = chunkStore.getExternalData().getWorld();
            AquariumService.dropStoredFish(world, origin, aquariumSize, fishItemId);
        }

        var world = chunkStore.getExternalData().getWorld();
        PersistentRef displayRef = aquarium.getDisplayReference();
        aquarium.setFishItemId(null);
        aquarium.setDisplayReference(null);
        chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);

        if (displayRef != null) {
            commandBuffer.run(store -> AquariumFishDisplaySpawner.despawnDisplayByRef(store, displayRef));
        }

        send(player, "server.cozytalefishing.aquarium.fish_removed");
        context.getState().state = InteractionState.Finished;
    }

    private static void removeLastDecoration(
        @Nonnull InteractionContext context,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nullable PlayerRef player,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull AquariumBlock aquarium,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize aquariumSize
    ) {
        int lastIndex = aquarium.getDecorationCount() - 1;
        PersistentRef displayRef = aquarium.getDecorationDisplayRef(lastIndex);
        String decorationItemId = aquarium.removeLastDecoration();
        if (decorationItemId == null) {
            send(player, "server.cozytalefishing.aquarium.empty");
            context.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack decorationStack = new ItemStack(decorationItemId, 1);
        var combinedInventory =
            InventoryComponent.getCombined(commandBuffer, playerRef, InventoryComponent.HOTBAR_FIRST);
        if (combinedInventory.canAddItemStack(decorationStack)) {
            combinedInventory.addItemStack(decorationStack);
        } else if (!Player.giveItem(decorationStack, playerRef, commandBuffer).succeeded()) {
            var world = chunkStore.getExternalData().getWorld();
            AquariumService.dropStoredDecorations(world, origin, aquariumSize, java.util.List.of(decorationItemId));
        }

        chunkStore.putComponent(blockRef, AquariumBlock.getComponentType(), aquarium);
        if (displayRef != null) {
            commandBuffer.run(store -> AquariumDecorationDisplaySpawner.despawnDisplayByRef(store, displayRef));
        }

        send(player, "server.cozytalefishing.aquarium.decoration_removed");
        context.getState().state = InteractionState.Finished;
    }

    private static void send(@Nullable PlayerRef player, @Nonnull String key) {
        if (player != null) {
            player.sendMessage(Message.translation(key));
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
