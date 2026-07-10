package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Per-aquarium-size decoration spot layout loaded from JSON. */
public final class AquariumLayoutAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, AquariumLayoutAsset>> {
    public static final BuilderCodec<SpotOffset> SPOT_CODEC =
        BuilderCodec.builder(SpotOffset.class, SpotOffset::new)
            .append(
                new KeyedCodec<>("Offset", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                (o, v) -> o.offset = toFloatArray(v),
                o -> toDoubleArray(o.offset)
            )
            .add()
            .build();

    @Nonnull
    public static final AssetBuilderCodec<String, AquariumLayoutAsset> CODEC =
        AssetBuilderCodec.builder(
                AquariumLayoutAsset.class,
                AquariumLayoutAsset::new,
                Codec.STRING,
                (asset, id) -> asset.id = id,
                asset -> asset.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data
            )
            .append(new KeyedCodec<>("MaxDecorations", Codec.INTEGER), (a, v) -> a.maxDecorations = v, a -> a.maxDecorations)
            .addValidator(Validators.greaterThanOrEqual(Integer.valueOf(1)))
            .add()
            .append(
                new KeyedCodec<>("Spots", new ArrayCodec<>(SPOT_CODEC, SpotOffset[]::new)),
                (a, v) -> a.spots = v != null ? v : new SpotOffset[0],
                a -> a.spots
            )
            .add()
            .afterDecode(AquariumLayoutAsset::afterDecode)
            .build();

    @Nullable
    private static DefaultAssetMap<String, AquariumLayoutAsset> assetMap;

    @Nonnull
    public static DefaultAssetMap<String, AquariumLayoutAsset> getAssetMap() {
        if (assetMap == null) {
            assetMap = AssetRegistry.getAssetStore(AquariumLayoutAsset.class).getAssetMap();
        }
        return assetMap;
    }

    protected com.hypixel.hytale.assetstore.AssetExtraInfo.Data data;
    protected String id = "Small";

    private int maxDecorations = 1;
    private SpotOffset[] spots = new SpotOffset[0];

    public AquariumLayoutAsset() {}

    private static void afterDecode(@Nonnull AquariumLayoutAsset asset) {
        if (asset.spots.length > 0 && asset.maxDecorations > asset.spots.length) {
            asset.maxDecorations = asset.spots.length;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public AquariumSize getAquariumSize() {
        return AquariumSize.fromString(id);
    }

    public int getMaxDecorations() {
        return maxDecorations;
    }

    @Nonnull
    public SpotOffset[] getSpots() {
        return spots;
    }

    @Nonnull
    public float[] getSpotOffset(int index) {
        if (index < 0 || index >= spots.length) {
            return new float[] {0.0f, 0.0f, 0.0f};
        }
        SpotOffset spot = spots[index];
        return spot != null && spot.offset != null ? spot.offset : new float[] {0.0f, 0.0f, 0.0f};
    }

    public static final class SpotOffset {
        private float[] offset = new float[] {0.0f, 0.0f, 0.0f};
    }

    @Nullable
    private static float[] toFloatArray(@Nullable Double[] values) {
        if (values == null || values.length == 0) {
            return new float[] {0.0f, 0.0f, 0.0f};
        }
        float[] out = new float[Math.min(values.length, 3)];
        for (int i = 0; i < out.length; i++) {
            out[i] = values[i] != null ? values[i].floatValue() : 0.0f;
        }
        return out;
    }

    @Nonnull
    private static Double[] toDoubleArray(@Nullable float[] values) {
        if (values == null || values.length == 0) {
            return new Double[] {0.0, 0.0, 0.0};
        }
        Double[] out = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (double) values[i];
        }
        return out;
    }
}
