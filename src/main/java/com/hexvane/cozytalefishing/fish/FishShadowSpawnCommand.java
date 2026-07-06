package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class FishShadowSpawnCommand extends AbstractPlayerCommand {
    public FishShadowSpawnCommand() {
        super("cozyfishspawnshadow", "Spawn a random fish shadow in nearby water.");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        requirePermission(HytalePermissions.fromCommand("cozyfish.spawnshadow"));
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        FishShadowSpawnHelper.SpawnResult result = FishShadowSpawnHelper.spawnRandomNearPlayer(store, ref, world);
        if (result == null) {
            context.sendMessage(Message.raw("Could not spawn a fish shadow — stand closer to water."));
            return;
        }
        context.sendMessage(Message.raw("Spawned " + result.species().getId() + " shadow nearby."));
    }
}
