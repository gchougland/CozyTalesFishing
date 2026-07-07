package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
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

public final class FishSpeciesAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, FishSpeciesAsset>> {
    @Nonnull
    public static final AssetBuilderCodec<String, FishSpeciesAsset> CODEC =
        AssetBuilderCodec.builder(
                FishSpeciesAsset.class,
                FishSpeciesAsset::new,
                Codec.STRING,
                (asset, id) -> asset.id = id,
                asset -> asset.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data
            )
            .append(new KeyedCodec<>("ShadowType", Codec.STRING), (a, v) -> a.shadowTypeRaw = v, a -> a.shadowTypeRaw)
            .addValidator(Validators.nonNull())
            .add()
            .append(new KeyedCodec<>("ItemId", Codec.STRING), (a, v) -> a.itemId = v, a -> a.itemId)
            .addValidator(Validators.nonNull())
            .addValidatorLate(() -> Item.VALIDATOR_CACHE.getValidator().late())
            .add()
            .append(new KeyedCodec<>("Rarity", Codec.STRING), (a, v) -> a.rarityRaw = v, a -> a.rarityRaw).add()
            .append(new KeyedCodec<>("SpawnWeight", Codec.INTEGER), (a, v) -> a.spawnWeight = v, a -> a.spawnWeight).add()
            .append(new KeyedCodec<>("SpawnLocation", FishSpawnLocation.CODEC), (a, v) -> a.spawnLocation = v, a -> a.spawnLocation)
            .addValidator(Validators.nonNull())
            .add()
            .append(
                new KeyedCodec<>("WaterBodyTypes", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (a, v) -> a.waterBodyTypesRaw = v,
                a -> a.waterBodyTypesRaw
            )
            .addValidator(Validators.nonNull())
            .add()
            .append(
                new KeyedCodec<>("DayTimeRange", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                (a, v) -> a.dayTimeRange = toFloatArray(v),
                a -> toDoubleArray(a.dayTimeRange)
            )
            .add()
            .append(
                new KeyedCodec<>("Weather", new ArrayCodec<>(Codec.STRING, String[]::new)),
                (a, v) -> a.weatherIds = v,
                a -> a.weatherIds
            )
            .add()
            .append(new KeyedCodec<>("UndergroundOnly", Codec.BOOLEAN), (a, v) -> a.undergroundOnly = v, a -> a.undergroundOnly).add()
            .append(new KeyedCodec<>("VisionRange", Codec.DOUBLE), (a, v) -> a.visionRange = v.floatValue(), a -> (double) a.visionRange).add()
            .append(new KeyedCodec<>("VisionAngleDegrees", Codec.DOUBLE), (a, v) -> a.visionAngleDegrees = v.floatValue(), a -> (double) a.visionAngleDegrees).add()
            .append(new KeyedCodec<>("PokeReachBlocks", Codec.DOUBLE), (a, v) -> a.pokeReachBlocks = v.floatValue(), a -> (double) a.pokeReachBlocks).add()
            .append(
                new KeyedCodec<>("SizeRangeCm", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                (a, v) -> a.sizeRangeCm = toFloatArray(v),
                a -> toDoubleArray(a.sizeRangeCm)
            )
            .add()
            .append(
                new KeyedCodec<>("ShadowScaleRange", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                (a, v) -> a.shadowScaleRange = toFloatArray(v),
                a -> toDoubleArray(a.shadowScaleRange)
            )
            .add()
            .append(new KeyedCodec<>("SwimSpeed", Codec.DOUBLE), (a, v) -> a.swimSpeed = v.floatValue(), a -> (double) a.swimSpeed).add()
            .append(new KeyedCodec<>("FleePlayerRange", Codec.DOUBLE), (a, v) -> a.fleePlayerRange = v.floatValue(), a -> (double) a.fleePlayerRange).add()
            .append(new KeyedCodec<>("FightSwimSpeed", Codec.DOUBLE), (a, v) -> a.fightSwimSpeed = v.floatValue(), a -> (double) a.fightSwimSpeed).add()
            .afterDecode(FishSpeciesAsset::afterDecode)
            .build();

    @Nullable
    private static DefaultAssetMap<String, FishSpeciesAsset> assetMap;

    @Nonnull
    public static DefaultAssetMap<String, FishSpeciesAsset> getAssetMap() {
        if (assetMap == null) {
            assetMap = AssetRegistry.getAssetStore(FishSpeciesAsset.class).getAssetMap();
        }
        return assetMap;
    }

    protected AssetExtraInfo.Data data;
    protected String id;

    private String shadowTypeRaw = "Small";
    private String itemId = "";
    private String rarityRaw = "Common";
    private int spawnWeight = 10;
    @Nullable
    private FishSpawnLocation spawnLocation;
    @Nullable
    private String[] waterBodyTypesRaw = new String[0];
    @Nullable
    private float[] dayTimeRange = new float[] {6.0f, 24.0f};
    @Nullable
    private String[] weatherIds = new String[0];
    private boolean undergroundOnly;
    private float visionRange = 4.0f;
    private float visionAngleDegrees = 110.0f;
    private float pokeReachBlocks = 0.35f;
    @Nullable
    private float[] sizeRangeCm = new float[] {10.0f, 20.0f};
    @Nullable
    private float[] shadowScaleRange = new float[] {0.8f, 1.1f};
    private float swimSpeed = 1.0f;
    private float fleePlayerRange = 3.5f;
    private float fightSwimSpeed = 1.4f;

    private FishShadowType shadowType = FishShadowType.Small;
    private FishRarity rarity = FishRarity.Common;
    @Nullable
    private WaterBodyType[] waterBodyTypes = new WaterBodyType[0];

    int[] allowedEnvironmentIndices = new int[0];
    boolean requiresUnderground;
    boolean requiresSurface;

    public FishSpeciesAsset() {}

    private static void afterDecode(@Nonnull FishSpeciesAsset asset) {
        FishShadowType type = FishShadowType.fromString(asset.shadowTypeRaw);
        asset.shadowType = type != null ? type : FishShadowType.Small;
        FishRarity parsedRarity = FishRarity.fromString(asset.rarityRaw);
        asset.rarity = parsedRarity != null ? parsedRarity : FishRarity.Common;
        if (asset.waterBodyTypesRaw != null) {
            WaterBodyType[] parsed = new WaterBodyType[asset.waterBodyTypesRaw.length];
            for (int i = 0; i < asset.waterBodyTypesRaw.length; i++) {
                WaterBodyType bodyType = WaterBodyType.fromString(asset.waterBodyTypesRaw[i]);
                parsed[i] = bodyType != null ? bodyType : WaterBodyType.Pond;
            }
            asset.waterBodyTypes = parsed;
        }
        asset.requiresUnderground = asset.undergroundOnly;
        asset.requiresSurface = !asset.undergroundOnly;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public FishShadowType getShadowType() {
        return shadowType;
    }

    @Nonnull
    public String getItemId() {
        return itemId;
    }

    @Nonnull
    public FishRarity getRarity() {
        return rarity;
    }

    public int getSpawnWeight() {
        return spawnWeight;
    }

    @Nonnull
    public FishSpawnLocation getSpawnLocation() {
        return spawnLocation != null ? spawnLocation : new FishSpawnLocation();
    }

    @Nonnull
    public WaterBodyType[] getWaterBodyTypes() {
        return waterBodyTypes != null ? waterBodyTypes : new WaterBodyType[0];
    }

    public boolean matchesWaterBody(@Nonnull WaterBodyType bodyType) {
        for (WaterBodyType allowed : getWaterBodyTypes()) {
            if (allowed == bodyType) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public float[] getDayTimeRange() {
        return dayTimeRange;
    }

    @Nullable
    public String[] getWeatherIds() {
        return weatherIds;
    }

    public boolean isUndergroundOnly() {
        return undergroundOnly;
    }

    public float getVisionRange() {
        return visionRange;
    }

    public float getVisionAngleDegrees() {
        return visionAngleDegrees;
    }

    public float getPokeReachBlocks() {
        return pokeReachBlocks;
    }

    @Nonnull
    public float[] getSizeRangeCm() {
        return sizeRangeCm != null ? sizeRangeCm : new float[] {10.0f, 20.0f};
    }

    @Nonnull
    public float[] getShadowScaleRange() {
        return shadowScaleRange != null ? shadowScaleRange : new float[] {0.8f, 1.1f};
    }

    public float getSwimSpeed() {
        return swimSpeed;
    }

    public float getFleePlayerRange() {
        return fleePlayerRange;
    }

    public float getFightSwimSpeed() {
        return fightSwimSpeed;
    }

    /** Extra horizontal run away from the player (beyond the hook distance) before the line breaks; scales with rarity. */
    public float getFightEscapeDistanceBlocks(@Nonnull FishingModConfig config) {
        float easy = config.getFightEscapeDistanceEasyBlocks();
        float hard = config.getFightEscapeDistanceHardBlocks();
        int maxOrdinal = FishRarity.values().length - 1;
        if (maxOrdinal <= 0) {
            return easy;
        }
        float t = rarity.ordinal() / (float) maxOrdinal;
        return easy + (hard - easy) * t;
    }

    public int[] getAllowedEnvironmentIndices() {
        return allowedEnvironmentIndices;
    }

    public float getEffectiveSpawnWeight(float globalMultiplier) {
        return spawnWeight * rarity.getSpawnMultiplier() * globalMultiplier;
    }

    @Nullable
    private static float[] toFloatArray(@Nullable Double[] values) {
        if (values == null) {
            return null;
        }
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] != null ? values[i].floatValue() : 0.0f;
        }
        return result;
    }

    @Nullable
    private static Double[] toDoubleArray(@Nullable float[] values) {
        if (values == null) {
            return null;
        }
        Double[] result = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (double) values[i];
        }
        return result;
    }
}
