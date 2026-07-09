package com.hexvane.cozytalefishing.bobber;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EquippedBobbersMetadata {
    private static final String METADATA_KEY = BobberConstants.METADATA_KEY;

    public static final BuilderCodec<EquippedBobbersMetadata> CODEC =
        BuilderCodec.builder(EquippedBobbersMetadata.class, EquippedBobbersMetadata::new)
            .append(
                new KeyedCodec<>("Slots", new ArrayCodec<>(ItemStack.CODEC, ItemStack[]::new)),
                (meta, slots) -> meta.slots = slots,
                meta -> meta.slots
            )
            .add()
            .build();

    public static final KeyedCodec<EquippedBobbersMetadata> KEYED_CODEC =
        new KeyedCodec<>(METADATA_KEY, CODEC);

    @Nullable
    private ItemStack[] slots = ItemStack.EMPTY_ARRAY;

    public EquippedBobbersMetadata() {}

    public EquippedBobbersMetadata(@Nonnull ItemStack[] slots) {
        this.slots = slots;
    }

    @Nonnull
    public ItemStack[] getSlots() {
        return slots == null ? ItemStack.EMPTY_ARRAY : slots;
    }

    @Nonnull
    public EquippedBobbersMetadata withSlots(@Nonnull ItemStack[] newSlots) {
        return new EquippedBobbersMetadata(Arrays.copyOf(newSlots, newSlots.length));
    }
}
