package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.fishing.FishingDebugLog;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.systems.NewSpawnStartTickingSystem;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class FishMonsterSpawnService {
    private FishMonsterSpawnService() {}

    public static void spawnAtCatch(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull String npcRoleId,
        @Nullable Vector3d catchPosition,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        if (npcRoleId.isBlank()) {
            FishingDebugLog.warn("Monster catch skipped: missing NpcRoleId");
            return;
        }

        Vector3d spawnPos = catchPosition;
        if (spawnPos == null) {
            TransformComponent playerTransform =
                commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
            if (playerTransform == null) {
                FishingDebugLog.warn("Monster catch skipped: no spawn position for role %s", npcRoleId);
                return;
            }
            spawnPos = new Vector3d(playerTransform.getPosition());
        }

        final Vector3d finalSpawnPos = new Vector3d(spawnPos);
        int roleIndex = NPCPlugin.get().getIndex(npcRoleId);
        if (roleIndex < 0) {
            FishingDebugLog.warn("Monster catch skipped: unknown NPC role %s", npcRoleId);
            return;
        }

        try {
            NPCPlugin.get().validateSpawnableRole(npcRoleId);
        } catch (RuntimeException e) {
            FishingDebugLog.warn("Monster catch skipped: role %s is not spawnable (%s)", npcRoleId, e.getMessage());
            return;
        }

        commandBuffer.run(
            store ->
                NPCPlugin.get().spawnEntity(
                    store,
                    roleIndex,
                    finalSpawnPos,
                    Rotation3f.IDENTITY,
                    null,
                    (npc, ref, s) -> NewSpawnStartTickingSystem.queueNewSpawn(ref, s)
                )
        );
    }
}
