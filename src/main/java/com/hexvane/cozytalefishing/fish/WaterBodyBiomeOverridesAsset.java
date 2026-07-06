package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WaterBodyBiomeOverridesAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, WaterBodyBiomeOverridesAsset>> {
    public static final String ASSET_ID = "WaterBodyBiomeOverrides";

    @Nonnull
    public static final AssetBuilderCodec<String, WaterBodyBiomeOverridesAsset> CODEC =
        AssetBuilderCodec.builder(
                WaterBodyBiomeOverridesAsset.class,
                WaterBodyBiomeOverridesAsset::new,
                Codec.STRING,
                (asset, id) -> asset.id = id,
                asset -> asset.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data
            )
            .append(
                new KeyedCodec<>("BiomePatterns", new MapCodec<>(Codec.STRING, HashMap::new)),
                (asset, map) -> asset.biomePatterns = map != null ? map : Collections.emptyMap(),
                asset -> asset.biomePatterns
            )
            .add()
            .afterDecode(WaterBodyBiomeOverridesAsset::afterDecode)
            .build();

    @Nullable
    private static DefaultAssetMap<String, WaterBodyBiomeOverridesAsset> assetMap;

    @Nonnull
    public static DefaultAssetMap<String, WaterBodyBiomeOverridesAsset> getAssetMap() {
        if (assetMap == null) {
            assetMap = AssetRegistry.getAssetStore(WaterBodyBiomeOverridesAsset.class).getAssetMap();
        }
        return assetMap;
    }

    protected com.hypixel.hytale.assetstore.AssetExtraInfo.Data data;
    protected String id = ASSET_ID;

    private Map<String, String> biomePatterns = Collections.emptyMap();
    private final Map<Integer, WaterBodyType> exactBiomeHashes = new HashMap<>();
    private final Map<String, WaterBodyType> substringPatterns = new HashMap<>();

    public WaterBodyBiomeOverridesAsset() {}

    private static void afterDecode(@Nonnull WaterBodyBiomeOverridesAsset asset) {
        asset.exactBiomeHashes.clear();
        asset.substringPatterns.clear();
        for (Map.Entry<String, String> entry : asset.biomePatterns.entrySet()) {
            WaterBodyType type = WaterBodyType.fromString(entry.getValue());
            if (type == null) {
                continue;
            }
            String key = entry.getKey();
            if (key.startsWith("*") && key.endsWith("*") && key.length() > 2) {
                asset.substringPatterns.put(key.substring(1, key.length() - 1).toLowerCase(), type);
            } else {
                asset.exactBiomeHashes.put(key.toLowerCase().hashCode(), type);
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public WaterBodyType resolveBiomeName(@Nonnull String biomeName) {
        WaterBodyType exact = exactBiomeHashes.get(biomeName.toLowerCase().hashCode());
        if (exact != null) {
            return exact;
        }
        String lower = biomeName.toLowerCase();
        for (Map.Entry<String, WaterBodyType> entry : substringPatterns.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nonnull
    public static WaterBodyBiomeOverridesAsset getOrEmpty() {
        WaterBodyBiomeOverridesAsset asset = getAssetMap().getAsset(ASSET_ID);
        return asset != null ? asset : new WaterBodyBiomeOverridesAsset();
    }
}
