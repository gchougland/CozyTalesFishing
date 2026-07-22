package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Declares a mod fluid (e.g. oil) as fishable; may be shipped from any asset pack. */
public final class FishableFluidAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, FishableFluidAsset>> {
    @Nonnull
    public static final AssetBuilderCodec<String, FishableFluidAsset> CODEC =
        AssetBuilderCodec.builder(
                FishableFluidAsset.class,
                FishableFluidAsset::new,
                Codec.STRING,
                (asset, id) -> asset.id = id,
                asset -> asset.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data
            )
            .append(
                new KeyedCodec<>("FluidIds", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (a, v) -> a.fluidIdsRaw = v,
                a -> a.fluidIdsRaw
            )
            .addValidator(Validators.nonNull())
            .add()
            .append(new KeyedCodec<>("HabitatId", Codec.STRING), (a, v) -> a.habitatId = v, a -> a.habitatId)
            .addValidator(Validators.nonNull())
            .add()
            .append(
                new KeyedCodec<>("JournalHabitatKey", Codec.STRING),
                (a, v) -> a.journalHabitatKey = v,
                a -> a.journalHabitatKey
            )
            .add()
            .build();

    @Nullable
    private static DefaultAssetMap<String, FishableFluidAsset> assetMap;

    @Nonnull
    public static DefaultAssetMap<String, FishableFluidAsset> getAssetMap() {
        if (assetMap == null) {
            assetMap = AssetRegistry.getAssetStore(FishableFluidAsset.class).getAssetMap();
        }
        return assetMap;
    }

    protected com.hypixel.hytale.assetstore.AssetExtraInfo.Data data;
    protected String id;
    @Nullable
    private String[] fluidIdsRaw = new String[0];
    @Nullable
    private String habitatId;
    @Nullable
    private String journalHabitatKey;

    public FishableFluidAsset() {}

    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public String[] getFluidIds() {
        return fluidIdsRaw != null ? fluidIdsRaw : new String[0];
    }

    @Nonnull
    public String getHabitatId() {
        return habitatId != null ? habitatId : id;
    }

    @Nullable
    public String getJournalHabitatKey() {
        return journalHabitatKey;
    }
}
