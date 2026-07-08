package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class FishCatchService {
    /**
     * Skews rolled size toward the minimum of {@link FishSpeciesAsset#getSizeRangeCm()}.
     * Values above 1 make larger sizes progressively rarer (2 ≈ median at 25% of the range).
     */
    private static final float SIZE_SKEW_EXPONENT = 2.0f;

    private FishCatchService() {}

    public static float rollSizeCm(@Nonnull FishSpeciesAsset species) {
        if (species.isTrash()) {
            return 0.0f;
        }
        float[] range = species.getSizeRangeCm();
        if (range.length < 2) {
            return range.length == 1 ? range[0] : 10.0f;
        }
        float min = Math.min(range[0], range[1]);
        float max = Math.max(range[0], range[1]);
        return rollSkewedSizeCm(min, max);
    }

    private static float rollSkewedSizeCm(float min, float max) {
        if (max <= min) {
            return min;
        }
        float span = max - min;
        float t = (float) Math.pow(Math.random(), SIZE_SKEW_EXPONENT);
        return min + t * span;
    }

    public static void notifyEscape(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull FishSpeciesAsset species
    ) {
        PlayerRef playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent != null) {
            playerRefComponent.sendMessage(
                Message
                    .translation("server.cozytalefishing.catch.escape")
                    .param("fish", FishSpeciesDisplayNames.resolve(species))
            );
        }
    }

    public static void completeCatch(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull FishSpeciesAsset species,
        float sizeCm,
        @Nullable FishShadowComponent shadow,
        @Nullable Vector3d catchPosition
    ) {
        ItemStack stack = new ItemStack(species.getItemId(), 1);
        Player.giveItem(stack, playerRef, commandBuffer);

        PlayerRef playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (species.isTrash()) {
            if (playerRefComponent != null) {
                playerRefComponent.sendMessage(
                    Message
                        .translation("server.cozytalefishing.catch.trash_success")
                        .param("fish", FishSpeciesDisplayNames.resolve(species))
                );
            }
            return;
        }

        FishCatchRecordComponent records = commandBuffer.getComponent(playerRef, FishCatchRecordComponent.getComponentType());
        if (records == null) {
            records = new FishCatchRecordComponent();
        }
        records.discover(species.getId());
        boolean personalBest = records.updateLargest(species.getId(), sizeCm);
        commandBuffer.putComponent(playerRef, FishCatchRecordComponent.getComponentType(), records);

        if (playerRefComponent != null) {
            Message message =
                Message
                    .translation("server.cozytalefishing.catch.success")
                    .param("fish", FishSpeciesDisplayNames.resolve(species))
                    .param("size", String.format("%.1f", sizeCm));
            playerRefComponent.sendMessage(message);
            if (personalBest) {
                playerRefComponent.sendMessage(Message.translation("server.cozytalefishing.catch.personal_best"));
            }
            FishSpeciesSpawnDebug.sendCatchDiagnosticsIfEnabled(
                commandBuffer,
                playerRef,
                playerRefComponent,
                species,
                shadow,
                catchPosition
            );
        }
    }

    public static void completeCatch(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull FishSpeciesAsset species,
        float sizeCm
    ) {
        completeCatch(commandBuffer, playerRef, species, sizeCm, null, null);
    }

}
