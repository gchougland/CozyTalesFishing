package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Persistent aquarium block state: stored fish, decorations, and linked display entities. */
public final class AquariumBlock implements Component<ChunkStore> {
    public static final BuilderCodec<AquariumBlock> CODEC =
        BuilderCodec.builder(AquariumBlock.class, AquariumBlock::new)
            .append(new KeyedCodec<>("FishItemId", Codec.STRING), (o, v) -> o.fishItemId = v, o -> o.fishItemId)
            .add()
            .append(
                new KeyedCodec<>("AquariumSize", Codec.STRING),
                (o, v) -> o.aquariumSize = AquariumSize.fromString(v),
                o -> o.aquariumSize != null ? switch (o.aquariumSize) {
                    case Small -> "Small";
                    case Wide2x1 -> "Wide2x1";
                    case Tall3x2x2 -> "Tall3x2x2";
                } : null
            )
            .add()
            .append(new KeyedCodec<>("RotationIndex", Codec.INTEGER), (o, v) -> o.rotationIndex = v, o -> o.rotationIndex)
            .add()
            .append(
                new KeyedCodec<>("DecorationItemIds", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (o, v) -> o.decorationItemIds = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                o -> o.decorationItemIds.isEmpty() ? null : o.decorationItemIds.toArray(String[]::new)
            )
            .add()
            .build();

    @Nullable
    private static ComponentType<ChunkStore, AquariumBlock> componentType;

    public static void register(@Nonnull ComponentType<ChunkStore, AquariumBlock> type) {
        componentType = type;
    }

    @Nonnull
    public static ComponentType<ChunkStore, AquariumBlock> getComponentType() {
        ComponentType<ChunkStore, AquariumBlock> type = componentType;
        if (type == null) {
            throw new IllegalStateException("AquariumBlock not registered");
        }
        return type;
    }

    @Nullable private String fishItemId;
    /** Runtime-only link to the spawned fish display entity; not saved to disk. */
    @Nullable private PersistentRef displayReference;
    @Nonnull private List<String> decorationItemIds = new ArrayList<>();
    /** Runtime-only links to spawned decoration display entities; not saved to disk. */
    @Nonnull private List<PersistentRef> decorationDisplayRefs = new ArrayList<>();
    @Nullable private AquariumSize aquariumSize;
    private int rotationIndex = -1;
    private float displayLostTimeout = 5.0f;
    private float decorationDisplayLostTimeout = 5.0f;

    public AquariumBlock() {}

    @Nullable
    public String getFishItemId() {
        return fishItemId;
    }

    public void setFishItemId(@Nullable String fishItemId) {
        this.fishItemId = fishItemId;
    }

    @Nullable
    public PersistentRef getDisplayReference() {
        return displayReference;
    }

    public void setDisplayReference(@Nullable PersistentRef displayReference) {
        this.displayReference = displayReference;
    }

    @Nonnull
    public List<String> getDecorationItemIds() {
        return decorationItemIds;
    }

    public int getDecorationCount() {
        return decorationItemIds.size();
    }

    public boolean hasDecorations() {
        return !decorationItemIds.isEmpty();
    }

    public boolean canAddDecoration(int maxDecorations) {
        return decorationItemIds.size() < maxDecorations;
    }

    public void addDecoration(@Nonnull String itemId) {
        decorationItemIds.add(itemId);
        ensureDecorationRefCapacity();
        decorationDisplayRefs.set(decorationItemIds.size() - 1, null);
    }

    @Nullable
    public String removeLastDecoration() {
        if (decorationItemIds.isEmpty()) {
            return null;
        }
        String removed = decorationItemIds.remove(decorationItemIds.size() - 1);
        if (!decorationDisplayRefs.isEmpty()) {
            decorationDisplayRefs.remove(decorationDisplayRefs.size() - 1);
        }
        return removed;
    }

    public void clearDecorationDisplayRefs() {
        decorationDisplayRefs.clear();
        for (int i = 0; i < decorationItemIds.size(); i++) {
            decorationDisplayRefs.add(null);
        }
    }

    @Nullable
    public PersistentRef getDecorationDisplayRef(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= decorationDisplayRefs.size()) {
            return null;
        }
        return decorationDisplayRefs.get(slotIndex);
    }

    public void setDecorationDisplayRef(int slotIndex, @Nullable PersistentRef displayReference) {
        ensureDecorationRefCapacity();
        if (slotIndex >= 0 && slotIndex < decorationDisplayRefs.size()) {
            decorationDisplayRefs.set(slotIndex, displayReference);
        }
    }

    @Nonnull
    public List<PersistentRef> getDecorationDisplayRefs() {
        return decorationDisplayRefs;
    }

    private void ensureDecorationRefCapacity() {
        while (decorationDisplayRefs.size() < decorationItemIds.size()) {
            decorationDisplayRefs.add(null);
        }
        while (decorationDisplayRefs.size() > decorationItemIds.size()) {
            decorationDisplayRefs.remove(decorationDisplayRefs.size() - 1);
        }
    }

    @Nullable
    public AquariumSize getAquariumSize() {
        return aquariumSize;
    }

    public void setAquariumSize(@Nullable AquariumSize aquariumSize) {
        this.aquariumSize = aquariumSize;
    }

    public int getRotationIndex() {
        return rotationIndex;
    }

    public void setRotationIndex(int rotationIndex) {
        this.rotationIndex = rotationIndex;
    }

    public boolean hasFish() {
        return fishItemId != null && !fishItemId.isBlank();
    }

    public void refreshDisplayLostTimeout() {
        displayLostTimeout = 5.0f;
    }

    public boolean tickDisplayLostTimeout(float dt) {
        return (displayLostTimeout -= dt) <= 0.0f;
    }

    public void refreshDecorationDisplayLostTimeout() {
        decorationDisplayLostTimeout = 5.0f;
    }

    public boolean tickDecorationDisplayLostTimeout(float dt) {
        return (decorationDisplayLostTimeout -= dt) <= 0.0f;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        AquariumBlock copy = new AquariumBlock();
        copy.fishItemId = fishItemId;
        copy.displayReference =
            displayReference != null ? new PersistentRef(displayReference.getUuid()) : null;
        copy.decorationItemIds = new ArrayList<>(decorationItemIds);
        copy.decorationDisplayRefs = cloneDecorationRefs(decorationDisplayRefs);
        copy.aquariumSize = aquariumSize;
        copy.rotationIndex = rotationIndex;
        copy.displayLostTimeout = displayLostTimeout;
        copy.decorationDisplayLostTimeout = decorationDisplayLostTimeout;
        return copy;
    }

    @Nonnull
    private static List<PersistentRef> cloneDecorationRefs(@Nonnull List<PersistentRef> refs) {
        List<PersistentRef> copy = new ArrayList<>(refs.size());
        for (PersistentRef ref : refs) {
            copy.add(ref != null ? new PersistentRef(ref.getUuid()) : null);
        }
        return copy;
    }
}
