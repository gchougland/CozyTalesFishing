package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingConstants;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class FishShadowProximity {
    private FishShadowProximity() {}

    public static boolean isHoldingFishingRod(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> playerRef) {
        ItemStack held = InventoryComponent.getItemInHand(commandBuffer, playerRef);
        return held != null && !held.isEmpty() && FishingConstants.FISHING_ROD_ITEM_ID.equals(held.getItemId());
    }

    /** True when any player holding the fishing rod is within horizontal range of the position. */
    public static boolean hasRodHolderNear(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d position,
        float radiusBlocks
    ) {
        double radiusSq = radiusBlocks * radiusBlocks;
        boolean[] found = {false};
        commandBuffer.getStore().forEachEntityParallel(
            Player.getComponentType(),
            (idx, chunk, parallelBuffer) -> {
                if (found[0]) {
                    return;
                }
                TransformComponent transform = chunk.getComponent(idx, TransformComponent.getComponentType());
                if (transform == null) {
                    return;
                }
                Ref<EntityStore> playerRef = chunk.getReferenceTo(idx);
                ItemStack held = InventoryComponent.getItemInHand(parallelBuffer, playerRef);
                if (held == null || held.isEmpty() || !FishingConstants.FISHING_ROD_ITEM_ID.equals(held.getItemId())) {
                    return;
                }
                Vector3d playerPos = transform.getPosition();
                double dx = playerPos.x - position.x;
                double dz = playerPos.z - position.z;
                if (dx * dx + dz * dz <= radiusSq) {
                    found[0] = true;
                }
            }
        );
        return found[0];
    }
}
