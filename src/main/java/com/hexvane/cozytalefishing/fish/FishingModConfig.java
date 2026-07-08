package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Runtime mod configuration loaded from the plugin data directory. */
public final class FishingModConfig {
    @Nonnull
    public static final BuilderCodec<FishingModConfig> CODEC =
        BuilderCodec.builder(FishingModConfig.class, FishingModConfig::new)
            .append(new KeyedCodec<>("ShadowsPerPlayerCap", Codec.INTEGER), (a, v) -> a.shadowsPerPlayerCap = v, a -> a.shadowsPerPlayerCap)
            .add()
            .append(
                new KeyedCodec<>("GlobalSpawnWeightMultiplier", Codec.DOUBLE),
                (a, v) -> a.globalSpawnWeightMultiplier = v.floatValue(),
                a -> (double) a.globalSpawnWeightMultiplier
            )
            .add()
            .append(
                new KeyedCodec<>("SpawnCheckIntervalSeconds", Codec.DOUBLE),
                (a, v) -> a.spawnCheckIntervalSeconds = v.floatValue(),
                a -> (double) a.spawnCheckIntervalSeconds
            )
            .add()
            .append(new KeyedCodec<>("SpawnRadiusMin", Codec.DOUBLE), (a, v) -> a.spawnRadiusMin = v.floatValue(), a -> (double) a.spawnRadiusMin)
            .add()
            .append(new KeyedCodec<>("SpawnRadiusMax", Codec.DOUBLE), (a, v) -> a.spawnRadiusMax = v.floatValue(), a -> (double) a.spawnRadiusMax)
            .add()
            .append(new KeyedCodec<>("MinWaterDepthBlocks", Codec.INTEGER), (a, v) -> a.minWaterDepthBlocks = v, a -> a.minWaterDepthBlocks)
            .add()
            .append(new KeyedCodec<>("UndergroundSurfaceOffset", Codec.INTEGER), (a, v) -> a.undergroundSurfaceOffset = v, a -> a.undergroundSurfaceOffset)
            .add()
            .append(
                new KeyedCodec<>("ReelSpeedBlocksPerSecond", Codec.DOUBLE),
                (a, v) -> a.reelSpeedBlocksPerSecond = v.floatValue(),
                a -> (double) a.reelSpeedBlocksPerSecond
            )
            .add()
            .append(
                new KeyedCodec<>("FightReelSpeedBlocksPerSecond", Codec.DOUBLE),
                (a, v) -> a.fightReelSpeedBlocksPerSecond = v.floatValue(),
                a -> (double) a.fightReelSpeedBlocksPerSecond
            )
            .add()
            .append(
                new KeyedCodec<>("FightReelResistanceFactor", Codec.DOUBLE),
                (a, v) -> a.fightReelResistanceFactor = v.floatValue(),
                a -> (double) a.fightReelResistanceFactor
            )
            .add()
            .append(
                new KeyedCodec<>("MaxFishingStamina", Codec.DOUBLE),
                (a, v) -> a.maxFishingStamina = v.floatValue(),
                a -> (double) a.maxFishingStamina
            )
            .add()
            .append(
                new KeyedCodec<>("FishingStaminaDrainPerSecond", Codec.DOUBLE),
                (a, v) -> a.fishingStaminaDrainPerSecond = v.floatValue(),
                a -> (double) a.fishingStaminaDrainPerSecond
            )
            .add()
            .append(
                new KeyedCodec<>("FishingStaminaRegenPerSecond", Codec.DOUBLE),
                (a, v) -> a.fishingStaminaRegenPerSecond = v.floatValue(),
                a -> (double) a.fishingStaminaRegenPerSecond
            )
            .add()
            .append(
                new KeyedCodec<>("FightDifficultyBaselineSwimSpeed", Codec.DOUBLE),
                (a, v) -> a.fightDifficultyBaselineSwimSpeed = v.floatValue(),
                a -> (double) a.fightDifficultyBaselineSwimSpeed
            )
            .add()
            .append(
                new KeyedCodec<>("FightDifficultyRarityStep", Codec.DOUBLE),
                (a, v) -> a.fightDifficultyRarityStep = v.floatValue(),
                a -> (double) a.fightDifficultyRarityStep
            )
            .add()
            .append(
                new KeyedCodec<>("FightReelCounterPullFactor", Codec.DOUBLE),
                (a, v) -> a.fightReelCounterPullFactor = v.floatValue(),
                a -> (double) a.fightReelCounterPullFactor
            )
            .add()
            .append(
                new KeyedCodec<>("CatchDistanceBlocks", Codec.DOUBLE),
                (a, v) -> a.catchDistanceBlocks = v.floatValue(),
                a -> (double) a.catchDistanceBlocks
            )
            .add()
            .append(
                new KeyedCodec<>("CatchMinReelBlocks", Codec.DOUBLE),
                (a, v) -> a.catchMinReelBlocks = v.floatValue(),
                a -> (double) a.catchMinReelBlocks
            )
            .add()
            .append(
                new KeyedCodec<>("RecallLineLengthBlocks", Codec.DOUBLE),
                (a, v) -> a.recallLineLengthBlocks = v.floatValue(),
                a -> (double) a.recallLineLengthBlocks
            )
            .add()
            .append(new KeyedCodec<>("FleeShrinkDurationSeconds", Codec.DOUBLE), (a, v) -> a.fleeShrinkDurationSeconds = v.floatValue(), a -> (double) a.fleeShrinkDurationSeconds)
            .add()
            .append(
                new KeyedCodec<>("FightEscapeDistanceEasyBlocks", Codec.DOUBLE),
                (a, v) -> a.fightEscapeDistanceEasyBlocks = v.floatValue(),
                a -> (double) a.fightEscapeDistanceEasyBlocks
            )
            .add()
            .append(
                new KeyedCodec<>("FightEscapeDistanceHardBlocks", Codec.DOUBLE),
                (a, v) -> a.fightEscapeDistanceHardBlocks = v.floatValue(),
                a -> (double) a.fightEscapeDistanceHardBlocks
            )
            .add()
            .append(
                new KeyedCodec<>("ShadowIdleDespawnSeconds", Codec.DOUBLE),
                (a, v) -> a.shadowIdleDespawnSeconds = v.floatValue(),
                a -> (double) a.shadowIdleDespawnSeconds
            )
            .add()
            .append(new KeyedCodec<>("WaterBodyCacheSize", Codec.INTEGER), (a, v) -> a.waterBodyCacheSize = v, a -> a.waterBodyCacheSize)
            .add()
            .append(new KeyedCodec<>("WaterBodyCellSize", Codec.INTEGER), (a, v) -> a.waterBodyCellSize = v, a -> a.waterBodyCellSize)
            .add()
            .append(new KeyedCodec<>("MaxFloodFillsPerSpawnCheck", Codec.INTEGER), (a, v) -> a.maxFloodFillsPerSpawnCheck = v, a -> a.maxFloodFillsPerSpawnCheck)
            .add()
            .append(new KeyedCodec<>("SpawnAttemptsPerCheck", Codec.INTEGER), (a, v) -> a.spawnAttemptsPerCheck = v, a -> a.spawnAttemptsPerCheck)
            .add()
            .append(new KeyedCodec<>("PondMaxBlocks", Codec.INTEGER), (a, v) -> a.pondMaxBlocks = v, a -> a.pondMaxBlocks)
            .add()
            .append(new KeyedCodec<>("PondMaxDimension", Codec.INTEGER), (a, v) -> a.pondMaxDimension = v, a -> a.pondMaxDimension)
            .add()
            .append(
                new KeyedCodec<>("RiverMinAspectRatio", Codec.DOUBLE),
                (a, v) -> a.riverMinAspectRatio = v.floatValue(),
                a -> (double) a.riverMinAspectRatio
            )
            .add()
            .append(new KeyedCodec<>("RiverMaxBlocks", Codec.INTEGER), (a, v) -> a.riverMaxBlocks = v, a -> a.riverMaxBlocks)
            .add()
            .append(new KeyedCodec<>("OceanMinBlocks", Codec.INTEGER), (a, v) -> a.oceanMinBlocks = v, a -> a.oceanMinBlocks)
            .add()
            .append(new KeyedCodec<>("OceanMinDepth", Codec.INTEGER), (a, v) -> a.oceanMinDepth = v, a -> a.oceanMinDepth)
            .add()
            .append(new KeyedCodec<>("FloodFillMaxBlocks", Codec.INTEGER), (a, v) -> a.floodFillMaxBlocks = v, a -> a.floodFillMaxBlocks)
            .add()
            .append(new KeyedCodec<>("FloodFillMaxRadius", Codec.INTEGER), (a, v) -> a.floodFillMaxRadius = v, a -> a.floodFillMaxRadius)
            .add()
            .append(new KeyedCodec<>("EnableSpawnDiagnostics", Codec.BOOLEAN), (a, v) -> a.enableSpawnDiagnostics = v, a -> a.enableSpawnDiagnostics)
            .add()
            .append(new KeyedCodec<>("EnableSpawnRegions", Codec.BOOLEAN), (a, v) -> a.enableSpawnRegions = v, a -> a.enableSpawnRegions)
            .add()
            .append(
                new KeyedCodec<>("RodTipBobAmplitude", Codec.DOUBLE),
                (a, v) -> a.rodTipBobAmplitude = v.floatValue(),
                a -> (double) a.rodTipBobAmplitude
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipBobFrequency", Codec.DOUBLE),
                (a, v) -> a.rodTipBobFrequency = v.floatValue(),
                a -> (double) a.rodTipBobFrequency
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipBobViewUpWeight", Codec.DOUBLE),
                (a, v) -> a.rodTipBobViewUpWeight = v.floatValue(),
                a -> (double) a.rodTipBobViewUpWeight
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipBobPhaseOffset", Codec.DOUBLE),
                (a, v) -> a.rodTipBobPhaseOffset = v.floatValue(),
                a -> (double) a.rodTipBobPhaseOffset
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipBobReelAnimSpeed", Codec.DOUBLE),
                (a, v) -> a.rodTipBobReelAnimSpeed = v.floatValue(),
                a -> (double) a.rodTipBobReelAnimSpeed
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipBobSeedFromWorldTime", Codec.BOOLEAN),
                (a, v) -> a.rodTipBobSeedFromWorldTime = v,
                a -> a.rodTipBobSeedFromWorldTime
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipAttachHorizontal", Codec.DOUBLE),
                (a, v) -> a.rodTipAttachHorizontal = v,
                a -> a.rodTipAttachHorizontal
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipAttachVertical", Codec.DOUBLE),
                (a, v) -> a.rodTipAttachVertical = v,
                a -> a.rodTipAttachVertical
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipShaftLengthScale", Codec.DOUBLE),
                (a, v) -> a.rodTipShaftLengthScale = v,
                a -> a.rodTipShaftLengthScale
            )
            .add()
            .append(
                new KeyedCodec<>("RodTipVerticalLift", Codec.DOUBLE),
                (a, v) -> a.rodTipVerticalLift = v,
                a -> a.rodTipVerticalLift
            )
            .add()
            .append(
                new KeyedCodec<>("WoodenTrashSpawnChance", Codec.DOUBLE),
                (a, v) -> a.woodenTrashSpawnChance = v.floatValue(),
                a -> (double) a.woodenTrashSpawnChance
            )
            .add()
            .append(
                new KeyedCodec<>("IronTrashSpawnChance", Codec.DOUBLE),
                (a, v) -> a.ironTrashSpawnChance = v.floatValue(),
                a -> (double) a.ironTrashSpawnChance
            )
            .add()
            .append(
                new KeyedCodec<>("TreasureSpawnChance", Codec.DOUBLE),
                (a, v) -> a.treasureSpawnChance = v.floatValue(),
                a -> (double) a.treasureSpawnChance
            )
            .add()
            .append(
                new KeyedCodec<>("SpawnPreferredMissWeightMultiplier", Codec.DOUBLE),
                (a, v) -> a.spawnPreferredMissWeightMultiplier = v.floatValue(),
                a -> (double) a.spawnPreferredMissWeightMultiplier
            )
            .add()
            .append(
                new KeyedCodec<>("SpawnPreferredMatchWeightMultiplier", Codec.DOUBLE),
                (a, v) -> a.spawnPreferredMatchWeightMultiplier = v.floatValue(),
                a -> (double) a.spawnPreferredMatchWeightMultiplier
            )
            .add()
            .build();

    @Nullable
    private static FishingModConfig instance;

    @Nonnull
    public static FishingModConfig get() {
        return instance != null ? instance : defaults();
    }

    public static void bind(@Nonnull FishingModConfig config) {
        instance = config;
    }

    @Nonnull
    public static FishingModConfig defaults() {
        return new FishingModConfig();
    }

    private int shadowsPerPlayerCap = 7;
    private float globalSpawnWeightMultiplier = 1.0f;
    private float spawnCheckIntervalSeconds = 4.0f;
    private float spawnRadiusMin = 4.0f;
    private float spawnRadiusMax = 24.0f;
    private int minWaterDepthBlocks = 1;
    private int undergroundSurfaceOffset = 6;
    private float reelSpeedBlocksPerSecond = 2.0f;
    private float fightReelSpeedBlocksPerSecond = 2.5f;
    /** Fraction of fight swim speed applied as resistance while the player reels. */
    private float fightReelResistanceFactor = 0.35f;
    private float maxFishingStamina = 100.0f;
    private float fishingStaminaDrainPerSecond = 25.0f;
    private float fishingStaminaRegenPerSecond = 20.0f;
    private float fightDifficultyBaselineSwimSpeed = 1.0f;
    private float fightDifficultyRarityStep = 0.08f;
    /** Fraction of fight swim speed applied as away-from-player pull while reeling. */
    private float fightReelCounterPullFactor = 0.25f;
    /** Line max length at which a hooked fish is landed (not player distance). */
    private float catchDistanceBlocks = 2.5f;
    /** Minimum line reeled in during a fight before a catch can complete. */
    private float catchMinReelBlocks = 1.5f;
    /** Line max length at which an empty bobber is recalled while reeling. */
    private float recallLineLengthBlocks = 1.5f;
    private float fleeShrinkDurationSeconds = 0.8f;
    /** Max extra horizontal run from the player (beyond hook distance) before a Common fish breaks line during a fight. */
    private float fightEscapeDistanceEasyBlocks = 4.0f;
    /** Max extra horizontal run from the player (beyond hook distance) before a Legendary fish breaks line during a fight. */
    private float fightEscapeDistanceHardBlocks = 1.5f;
    /** Wandering shadows shrink/despawn after this long with no rod-holder in spawn radius. 0 disables. */
    private float shadowIdleDespawnSeconds = 90.0f;
    private int waterBodyCacheSize = 512;
    private int waterBodyCellSize = 16;
    private int maxFloodFillsPerSpawnCheck = 8;
    private int spawnAttemptsPerCheck = 8;
    private int pondMaxBlocks = 40;
    private int pondMaxDimension = 12;
    private float riverMinAspectRatio = 3.0f;
    private int riverMaxBlocks = 200;
    private int oceanMinBlocks = 200;
    private int oceanMinDepth = 8;
    private int floodFillMaxBlocks = 256;
    private int floodFillMaxRadius = 16;
    private boolean enableSpawnDiagnostics = false;
    private boolean enableSpawnRegions = true;
    /** Temporary tuning values for rod-tip idle bob; move to constants once dialed in. */
    private float rodTipBobAmplitude = 0.1f;
    private float rodTipBobFrequency = 0.5f;
    /** 1 = camera view-up, 0 = world Y. */
    private float rodTipBobViewUpWeight = 1.0f;
    /** Phase offset in cycles (0–1). Shifts bob timing to match the held rod animation. */
    private float rodTipBobPhaseOffset = 0.25f;
    /** Matches {@code ReelIn.Speed} in CozyFishingRod.json while the player is reeling. */
    private float rodTipBobReelAnimSpeed = 1.0f;
    /** Seed bob phase from server time when the rod is first equipped. */
    private boolean rodTipBobSeedFromWorldTime = true;
    /** Temporary rod-tip attach tuning; move to constants once dialed in. */
    private double rodTipAttachHorizontal = 0.3;
    private double rodTipAttachVertical = -0.18;
    private double rodTipShaftLengthScale = 0.7;
    private double rodTipVerticalLift = 0.85;
    private float woodenTrashSpawnChance = 0.15f;
    private float ironTrashSpawnChance = 0.05f;
    private float treasureSpawnChance = 0.03f;
    private float spawnPreferredMissWeightMultiplier = 0.35f;
    private float spawnPreferredMatchWeightMultiplier = 1.25f;

    public int getShadowsPerPlayerCap() {
        return shadowsPerPlayerCap;
    }

    public float getGlobalSpawnWeightMultiplier() {
        return globalSpawnWeightMultiplier;
    }

    public float getSpawnCheckIntervalSeconds() {
        return spawnCheckIntervalSeconds;
    }

    public float getSpawnRadiusMin() {
        return spawnRadiusMin;
    }

    public float getSpawnRadiusMax() {
        return spawnRadiusMax;
    }

    public int getMinWaterDepthBlocks() {
        return minWaterDepthBlocks;
    }

    public int getUndergroundSurfaceOffset() {
        return undergroundSurfaceOffset;
    }

    public float getReelSpeedBlocksPerSecond() {
        return reelSpeedBlocksPerSecond;
    }

    public float getFightReelSpeedBlocksPerSecond() {
        return fightReelSpeedBlocksPerSecond;
    }

    public float getFightReelResistanceFactor() {
        return fightReelResistanceFactor;
    }

    public float getMaxFishingStamina() {
        return maxFishingStamina;
    }

    public float getFishingStaminaDrainPerSecond() {
        return fishingStaminaDrainPerSecond;
    }

    public float getFishingStaminaRegenPerSecond() {
        return fishingStaminaRegenPerSecond;
    }

    public float getFightDifficultyBaselineSwimSpeed() {
        return fightDifficultyBaselineSwimSpeed;
    }

    public float getFightDifficultyRarityStep() {
        return fightDifficultyRarityStep;
    }

    public float getFightReelCounterPullFactor() {
        return fightReelCounterPullFactor;
    }

    public float getCatchDistanceBlocks() {
        return catchDistanceBlocks;
    }

    public float getCatchMinReelBlocks() {
        return catchMinReelBlocks;
    }

    public float getRecallLineLengthBlocks() {
        return recallLineLengthBlocks;
    }

    public float getFleeShrinkDurationSeconds() {
        return fleeShrinkDurationSeconds;
    }

    public float getFightEscapeDistanceEasyBlocks() {
        return fightEscapeDistanceEasyBlocks;
    }

    public float getFightEscapeDistanceHardBlocks() {
        return fightEscapeDistanceHardBlocks;
    }

    public float getShadowIdleDespawnSeconds() {
        return shadowIdleDespawnSeconds;
    }

    public int getWaterBodyCacheSize() {
        return waterBodyCacheSize;
    }

    public int getWaterBodyCellSize() {
        return waterBodyCellSize;
    }

    public int getMaxFloodFillsPerSpawnCheck() {
        return maxFloodFillsPerSpawnCheck;
    }

    public int getSpawnAttemptsPerCheck() {
        return spawnAttemptsPerCheck;
    }

    public int getPondMaxBlocks() {
        return pondMaxBlocks;
    }

    public int getPondMaxDimension() {
        return pondMaxDimension;
    }

    public float getRiverMinAspectRatio() {
        return riverMinAspectRatio;
    }

    public int getRiverMaxBlocks() {
        return riverMaxBlocks;
    }

    public int getOceanMinBlocks() {
        return oceanMinBlocks;
    }

    public int getOceanMinDepth() {
        return oceanMinDepth;
    }

    public int getFloodFillMaxBlocks() {
        return floodFillMaxBlocks;
    }

    public int getFloodFillMaxRadius() {
        return floodFillMaxRadius;
    }

    public boolean isEnableSpawnDiagnostics() {
        return enableSpawnDiagnostics;
    }

    public boolean isEnableSpawnRegions() {
        return enableSpawnRegions;
    }

    public float getRodTipBobAmplitude() {
        return rodTipBobAmplitude;
    }

    public float getRodTipBobFrequency() {
        return rodTipBobFrequency;
    }

    public float getRodTipBobViewUpWeight() {
        return rodTipBobViewUpWeight;
    }

    public float getRodTipBobPhaseOffset() {
        return rodTipBobPhaseOffset;
    }

    public float getRodTipBobReelAnimSpeed() {
        return rodTipBobReelAnimSpeed;
    }

    public boolean isRodTipBobSeedFromWorldTime() {
        return rodTipBobSeedFromWorldTime;
    }

    public double getRodTipAttachHorizontal() {
        return rodTipAttachHorizontal;
    }

    public double getRodTipAttachVertical() {
        return rodTipAttachVertical;
    }

    public double getRodTipShaftLengthScale() {
        return rodTipShaftLengthScale;
    }

    public double getRodTipVerticalLift() {
        return rodTipVerticalLift;
    }

    public float getWoodenTrashSpawnChance() {
        return woodenTrashSpawnChance;
    }

    public float getIronTrashSpawnChance() {
        return ironTrashSpawnChance;
    }

    public float getTreasureSpawnChance() {
        return treasureSpawnChance;
    }

    public float getSpawnPreferredMissWeightMultiplier() {
        return spawnPreferredMissWeightMultiplier;
    }

    public float getSpawnPreferredMatchWeightMultiplier() {
        return spawnPreferredMatchWeightMultiplier;
    }
}
