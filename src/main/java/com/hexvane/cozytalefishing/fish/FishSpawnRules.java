package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Unified spawn requirements with per-condition strictness. */
public final class FishSpawnRules {
    @Nonnull
    public static final BuilderCodec<FishSpawnRules> CODEC =
        BuilderCodec.builder(FishSpawnRules.class, FishSpawnRules::new)
            .append(new KeyedCodec<>("WaterBody", WaterBodyRule.CODEC), (r, v) -> r.waterBody = v, r -> r.waterBody)
            .add()
            .append(new KeyedCodec<>("Location", LocationRule.CODEC), (r, v) -> r.location = v, r -> r.location)
            .add()
            .append(new KeyedCodec<>("Underground", UndergroundRule.CODEC), (r, v) -> r.underground = v, r -> r.underground)
            .add()
            .append(new KeyedCodec<>("DayTime", DayTimeRule.CODEC), (r, v) -> r.dayTime = v, r -> r.dayTime)
            .add()
            .append(new KeyedCodec<>("Weather", WeatherRule.CODEC), (r, v) -> r.weather = v, r -> r.weather)
            .add()
            .append(new KeyedCodec<>("Fluid", FluidRule.CODEC), (r, v) -> r.fluid = v, r -> r.fluid)
            .add()
            .afterDecode(FishSpawnRules::afterDecode)
            .build();

    @Nullable
    private WaterBodyRule waterBody;
    @Nullable
    private LocationRule location;
    @Nullable
    private UndergroundRule underground;
    @Nullable
    private DayTimeRule dayTime;
    @Nullable
    private WeatherRule weather;
    @Nullable
    private FluidRule fluid;

    @Nonnull
    private WaterBodyType[] waterBodyTypes = new WaterBodyType[0];
    @Nonnull
    private int[] weatherIndexes = new int[0];

    public FishSpawnRules() {}

    @Nonnull
    public static FishSpawnRules fromLegacy(@Nonnull FishSpeciesAsset asset) {
        FishSpawnRules rules = new FishSpawnRules();

        WaterBodyRule waterBodyRule = new WaterBodyRule();
        waterBodyRule.typesRaw = asset.getLegacyWaterBodyTypesRaw();
        waterBodyRule.mode = FishSpawnRuleMode.Required;
        rules.waterBody = waterBodyRule;

        LocationRule locationRule = new LocationRule();
        FishSpawnLocation legacyLocation = asset.getLegacySpawnLocation();
        if (legacyLocation != null) {
            locationRule.zone = legacyLocation.getZone();
            locationRule.environments = legacyLocation.getEnvironments();
            locationRule.biomes = legacyLocation.getBiomes();
        }
        locationRule.zoneMode = locationRule.hasZone() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        locationRule.environmentMode =
            locationRule.hasEnvironments() || locationRule.hasBiomes() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        rules.location = locationRule;

        UndergroundRule undergroundRule = new UndergroundRule();
        undergroundRule.only = asset.getLegacyUndergroundOnly();
        undergroundRule.mode = FishSpawnRuleMode.Required;
        rules.underground = undergroundRule;

        DayTimeRule dayTimeRule = new DayTimeRule();
        float[] legacyRange = asset.getLegacyDayTimeRange();
        if (legacyRange != null && legacyRange.length >= 2) {
            dayTimeRule.range = Arrays.copyOf(legacyRange, legacyRange.length);
        }
        dayTimeRule.mode = inferLegacyDayTimeMode(dayTimeRule.range);
        rules.dayTime = dayTimeRule;

        WeatherRule weatherRule = new WeatherRule();
        weatherRule.ids = asset.getLegacyWeatherIds();
        weatherRule.mode = weatherRule.hasIds() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        rules.weather = weatherRule;

        rules.afterDecodeInternal();
        return rules;
    }

    private static void afterDecode(@Nonnull FishSpawnRules rules) {
        rules.afterDecodeInternal();
    }

    private void afterDecodeInternal() {
        ensureDefaults();
        parseWaterBodyTypes();
        weatherIndexes = resolveWeatherIndexes(weather != null ? weather.ids : null);
        if (weather != null) {
            weather.weatherIndexes = weatherIndexes;
            if (weather.mode == null) {
                weather.mode = weather.hasIds() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
            }
        }
    }

    private void ensureDefaults() {
        if (waterBody == null) {
            waterBody = new WaterBodyRule();
        }
        if (waterBody.mode == null) {
            waterBody.mode = FishSpawnRuleMode.Required;
        }

        if (location == null) {
            location = new LocationRule();
        }
        if (location.zoneMode == null) {
            location.zoneMode = location.hasZone() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        }
        if (location.environmentMode == null) {
            location.environmentMode =
                location.hasEnvironments() || location.hasBiomes() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        }

        if (underground == null) {
            underground = new UndergroundRule();
        }
        if (underground.mode == null) {
            underground.mode = underground.only != null ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        }

        if (dayTime == null) {
            dayTime = new DayTimeRule();
        }
        if (dayTime.mode == null) {
            dayTime.mode = inferLegacyDayTimeMode(dayTime.range);
        }

        if (weather == null) {
            weather = new WeatherRule();
        }
        if (weather.mode == null) {
            weather.mode = weather.hasIds() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        }

        if (fluid == null) {
            fluid = new FluidRule();
        }
        if (fluid.mode == null) {
            fluid.mode = fluid.hasIds() ? FishSpawnRuleMode.Required : FishSpawnRuleMode.Ignored;
        }
    }

    @Nullable
    private static FishSpawnRuleMode inferLegacyDayTimeMode(@Nullable float[] range) {
        if (range == null || range.length < 2) {
            return FishSpawnRuleMode.Ignored;
        }
        if (Math.abs(range[0] - 6.0f) < 0.01f && Math.abs(range[1] - 24.0f) < 0.01f) {
            return FishSpawnRuleMode.Preferred;
        }
        return FishSpawnRuleMode.Required;
    }

    private void parseWaterBodyTypes() {
        if (waterBody == null || waterBody.typesRaw == null) {
            waterBodyTypes = new WaterBodyType[0];
            return;
        }
        java.util.List<WaterBodyType> parsed = new java.util.ArrayList<>();
        for (String raw : waterBody.typesRaw) {
            WaterBodyType bodyType = WaterBodyType.fromString(raw);
            if (bodyType != null) {
                parsed.add(bodyType);
            }
        }
        waterBodyTypes = parsed.toArray(new WaterBodyType[0]);
    }

    @Nonnull
    private static int[] resolveWeatherIndexes(@Nullable String[] weatherIds) {
        if (weatherIds == null || weatherIds.length == 0) {
            return new int[0];
        }
        int[] indexes = new int[weatherIds.length];
        int count = 0;
        for (String weatherId : weatherIds) {
            if (weatherId == null || weatherId.isEmpty()) {
                continue;
            }
            int index = Weather.getAssetMap().getIndex(weatherId);
            if (index == AssetMapWithIndexes.NOT_FOUND) {
                continue;
            }
            indexes[count++] = index;
        }
        if (count == 0) {
            return new int[0];
        }
        if (count < indexes.length) {
            indexes = Arrays.copyOf(indexes, count);
        }
        Arrays.sort(indexes);
        return indexes;
    }

    @Nonnull
    public WaterBodyRule getWaterBody() {
        return waterBody != null ? waterBody : new WaterBodyRule();
    }

    @Nonnull
    public LocationRule getLocation() {
        return location != null ? location : new LocationRule();
    }

    @Nonnull
    public UndergroundRule getUnderground() {
        return underground != null ? underground : new UndergroundRule();
    }

    @Nonnull
    public DayTimeRule getDayTime() {
        return dayTime != null ? dayTime : new DayTimeRule();
    }

    @Nonnull
    public WeatherRule getWeather() {
        return weather != null ? weather : new WeatherRule();
    }

    @Nonnull
    public WaterBodyType[] getWaterBodyTypes() {
        return waterBodyTypes;
    }

    @Nonnull
    public int[] getWeatherIndexes() {
        return weatherIndexes;
    }

    public boolean matchesWaterBody(@Nonnull WaterBodyType bodyType) {
        for (WaterBodyType allowed : waterBodyTypes) {
            if (allowed == bodyType) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public FluidRule getFluid() {
        return fluid != null ? fluid : new FluidRule();
    }

    public boolean matchesFluid(@Nullable String columnFluidAssetId) {
        FluidRule rule = getFluid();
        if (!rule.hasIds()) {
            return false;
        }
        if (columnFluidAssetId == null || columnFluidAssetId.isBlank()) {
            return false;
        }
        for (String id : rule.ids) {
            if (id != null && FishableFluidRegistry.matchesFluidRule(id, columnFluidAssetId)) {
                return true;
            }
        }
        return false;
    }

    public boolean indexesByFluid() {
        FluidRule rule = getFluid();
        FishSpawnRuleMode mode = rule.mode != null ? rule.mode : FishSpawnRuleMode.Ignored;
        return mode != FishSpawnRuleMode.Ignored && rule.hasIds();
    }

    @Nonnull
    public FishSpawnLocation toSpawnLocation() {
        FishSpawnLocation spawnLocation = new FishSpawnLocation();
        LocationRule loc = getLocation();
        spawnLocation.setZone(loc.zone);
        spawnLocation.setEnvironments(loc.environments);
        spawnLocation.setBiomes(loc.biomes);
        return spawnLocation;
    }

    public boolean isUndergroundOnly() {
        UndergroundRule rule = getUnderground();
        return rule.only != null && rule.only;
    }

    @Nullable
    public float[] getDayTimeRange() {
        DayTimeRule rule = getDayTime();
        return rule.range != null ? rule.range : null;
    }

    @Nullable
    public String[] getWeatherIds() {
        WeatherRule rule = getWeather();
        return rule.ids;
    }

    public static final class WaterBodyRule {
        @Nonnull
        public static final BuilderCodec<WaterBodyRule> CODEC =
            BuilderCodec.builder(WaterBodyRule.class, WaterBodyRule::new)
                .append(
                    new KeyedCodec<>("Types", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (r, v) -> r.typesRaw = v,
                    r -> r.typesRaw
                )
                .add()
                .append(
                    new KeyedCodec<>("Mode", Codec.STRING),
                    (r, v) -> r.mode = FishSpawnRuleMode.fromString(v),
                    r -> r.mode != null ? r.mode.name() : null
                )
                .add()
                .build();

        @Nullable
        String[] typesRaw;
        @Nullable
        FishSpawnRuleMode mode;

        @Nullable
        public String[] getTypesRaw() {
            return typesRaw;
        }

        @Nullable
        public FishSpawnRuleMode getMode() {
            return mode;
        }
    }

    public static final class LocationRule {
        @Nonnull
        public static final BuilderCodec<LocationRule> CODEC =
            BuilderCodec.builder(LocationRule.class, LocationRule::new)
                .append(new KeyedCodec<>("Zone", Codec.STRING), (r, v) -> r.zone = v, r -> r.zone)
                .add()
                .append(
                    new KeyedCodec<>("Environments", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (r, v) -> r.environments = v,
                    r -> r.environments
                )
                .add()
                .append(
                    new KeyedCodec<>("Biomes", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (r, v) -> r.biomes = v,
                    r -> r.biomes
                )
                .add()
                .append(
                    new KeyedCodec<>("ZoneMode", Codec.STRING),
                    (r, v) -> r.zoneMode = FishSpawnRuleMode.fromString(v),
                    r -> r.zoneMode != null ? r.zoneMode.name() : null
                )
                .add()
                .append(
                    new KeyedCodec<>("EnvironmentMode", Codec.STRING),
                    (r, v) -> r.environmentMode = FishSpawnRuleMode.fromString(v),
                    r -> r.environmentMode != null ? r.environmentMode.name() : null
                )
                .add()
                .build();

        @Nullable
        String zone;
        @Nullable
        String[] environments;
        @Nullable
        String[] biomes;
        @Nullable
        FishSpawnRuleMode zoneMode;
        @Nullable
        FishSpawnRuleMode environmentMode;

        public boolean hasZone() {
            return zone != null && !zone.isBlank();
        }

        public boolean hasEnvironments() {
            return environments != null && environments.length > 0;
        }

        public boolean hasBiomes() {
            return biomes != null && biomes.length > 0;
        }

        @Nullable
        public String getZone() {
            return zone;
        }

        @Nullable
        public String[] getEnvironments() {
            return environments;
        }

        @Nullable
        public String[] getBiomes() {
            return biomes;
        }

        @Nullable
        public FishSpawnRuleMode getZoneMode() {
            return zoneMode;
        }

        @Nullable
        public FishSpawnRuleMode getEnvironmentMode() {
            return environmentMode;
        }
    }

    public static final class UndergroundRule {
        @Nonnull
        public static final BuilderCodec<UndergroundRule> CODEC =
            BuilderCodec.builder(UndergroundRule.class, UndergroundRule::new)
                .append(new KeyedCodec<>("Only", Codec.BOOLEAN), (r, v) -> r.only = v, r -> r.only)
                .add()
                .append(
                    new KeyedCodec<>("Mode", Codec.STRING),
                    (r, v) -> r.mode = FishSpawnRuleMode.fromString(v),
                    r -> r.mode != null ? r.mode.name() : null
                )
                .add()
                .build();

        @Nullable
        Boolean only;
        @Nullable
        FishSpawnRuleMode mode;

        @Nullable
        public Boolean getOnly() {
            return only;
        }

        @Nullable
        public FishSpawnRuleMode getMode() {
            return mode;
        }
    }

    public static final class DayTimeRule {
        @Nonnull
        public static final BuilderCodec<DayTimeRule> CODEC =
            BuilderCodec.builder(DayTimeRule.class, DayTimeRule::new)
                .append(
                    new KeyedCodec<>("Range", new ArrayCodec<>(Codec.DOUBLE, Double[]::new)),
                    (r, v) -> r.range = toFloatArray(v),
                    r -> toDoubleArray(r.range)
                )
                .add()
                .append(
                    new KeyedCodec<>("Mode", Codec.STRING),
                    (r, v) -> r.mode = FishSpawnRuleMode.fromString(v),
                    r -> r.mode != null ? r.mode.name() : null
                )
                .add()
                .build();

        @Nullable
        float[] range;
        @Nullable
        FishSpawnRuleMode mode;

        @Nullable
        public float[] getRange() {
            return range;
        }

        @Nullable
        public FishSpawnRuleMode getMode() {
            return mode;
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

    public static final class WeatherRule {
        @Nonnull
        public static final BuilderCodec<WeatherRule> CODEC =
            BuilderCodec.builder(WeatherRule.class, WeatherRule::new)
                .append(
                    new KeyedCodec<>("Ids", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (r, v) -> r.ids = v,
                    r -> r.ids
                )
                .add()
                .append(
                    new KeyedCodec<>("Mode", Codec.STRING),
                    (r, v) -> r.mode = FishSpawnRuleMode.fromString(v),
                    r -> r.mode != null ? r.mode.name() : null
                )
                .add()
                .build();

        @Nullable
        String[] ids;
        @Nullable
        FishSpawnRuleMode mode;
        @Nonnull
        int[] weatherIndexes = new int[0];

        public boolean hasIds() {
            return ids != null && ids.length > 0;
        }

        @Nullable
        public String[] getIds() {
            return ids;
        }

        @Nullable
        public FishSpawnRuleMode getMode() {
            return mode;
        }

        @Nonnull
        public int[] getWeatherIndexes() {
            return weatherIndexes;
        }
    }

    public static final class FluidRule {
        @Nonnull
        public static final BuilderCodec<FluidRule> CODEC =
            BuilderCodec.builder(FluidRule.class, FluidRule::new)
                .append(
                    new KeyedCodec<>("Ids", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (r, v) -> r.ids = v,
                    r -> r.ids
                )
                .add()
                .append(
                    new KeyedCodec<>("Mode", Codec.STRING),
                    (r, v) -> r.mode = FishSpawnRuleMode.fromString(v),
                    r -> r.mode != null ? r.mode.name() : null
                )
                .add()
                .build();

        @Nullable
        String[] ids;
        @Nullable
        FishSpawnRuleMode mode;

        public boolean hasIds() {
            return ids != null && ids.length > 0;
        }

        @Nullable
        public String[] getIds() {
            return ids;
        }

        @Nullable
        public FishSpawnRuleMode getMode() {
            return mode;
        }
    }
}
