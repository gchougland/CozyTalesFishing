package com.hexvane.cozytalefishing.aquarium;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Config for an item that can be placed as an aquarium decoration. */
public final class AquariumDecorationAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, AquariumDecorationAsset>> {
    public static final float DEFAULT_DISPLAY_SCALE = 1.40f;
    private static final float[] DEFAULT_DISPLAY_OFFSET = new float[] {0.0f, -0.12f, 0.0f};

    @Nonnull
    public static final AssetBuilderCodec<String, AquariumDecorationAsset> CODEC =
        AssetBuilderCodec.builder(
                AquariumDecorationAsset.class,
                AquariumDecorationAsset::new,
                Codec.STRING,
                (asset, id) -> asset.id = id,
                asset -> asset.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data
            )
            .append(new KeyedCodec<>("ItemId", Codec.STRING), (a, v) -> a.itemId = v, a -> a.itemId)
            .addValidator(Validators.nonNull())
            .addValidatorLate(() -> Item.VALIDATOR_CACHE.getValidator().late())
            .add()
            .append(
                new KeyedCodec<>("DisplayScale", Codec.DOUBLE),
                (a, v) -> a.displayScale = v.floatValue(),
                a -> (double) a.displayScale
            )
            .add()
            .append(
                new KeyedCodec<>("DisplayOffset", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                (a, v) -> a.displayOffset = toFloatArray(v),
                a -> toDoubleArray(a.displayOffset)
            )
            .add()
            .append(
                new KeyedCodec<>("DisplayYawOffset", Codec.DOUBLE),
                (a, v) -> a.displayYawOffset = v.floatValue(),
                a -> (double) a.displayYawOffset
            )
            .add()
            .build();

    @Nullable
    private static DefaultAssetMap<String, AquariumDecorationAsset> assetMap;

    @Nonnull
    public static DefaultAssetMap<String, AquariumDecorationAsset> getAssetMap() {
        if (assetMap == null) {
            assetMap = AssetRegistry.getAssetStore(AquariumDecorationAsset.class).getAssetMap();
        }
        return assetMap;
    }

    protected com.hypixel.hytale.assetstore.AssetExtraInfo.Data data;
    protected String id = "";

    private String itemId = "";
    private float displayScale = DEFAULT_DISPLAY_SCALE;
    @Nullable private float[] displayOffset;
    private float displayYawOffset = 0.0f;

    public AquariumDecorationAsset() {}

    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public String getItemId() {
        return itemId;
    }

    public float getDisplayScale() {
        return displayScale > 0.0f ? displayScale : DEFAULT_DISPLAY_SCALE;
    }

    @Nonnull
    public float[] getDisplayOffset() {
        return displayOffset != null ? displayOffset : DEFAULT_DISPLAY_OFFSET;
    }

    public float getDisplayYawOffset() {
        return displayYawOffset;
    }

    @Nullable
    private static float[] toFloatArray(@Nullable Double[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        float[] out = new float[Math.min(values.length, 3)];
        for (int i = 0; i < out.length; i++) {
            out[i] = values[i] != null ? values[i].floatValue() : 0.0f;
        }
        return out;
    }

    @Nullable
    private static Double[] toDoubleArray(@Nullable float[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        Double[] out = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (double) values[i];
        }
        return out;
    }
}
