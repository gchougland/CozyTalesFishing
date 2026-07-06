package com.hexvane.cozytalefishing.fish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class FishingSpawnRegionStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FishingSpawnRegionStorage() {}

    @Nonnull
    static Path worldFile(@Nonnull Path dataDirectory, @Nonnull UUID worldUuid) {
        return dataDirectory.resolve("spawn-regions").resolve(worldUuid + ".json");
    }

    @Nonnull
    static List<FishingSpawnRegion> load(@Nonnull Path file) throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        if (json.isBlank()) {
            return new ArrayList<>();
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray regionsArray = root.getAsJsonArray("Regions");
        if (regionsArray == null) {
            return new ArrayList<>();
        }
        List<FishingSpawnRegion> regions = new ArrayList<>();
        for (JsonElement element : regionsArray) {
            FishingSpawnRegion region = parseRegion(element.getAsJsonObject());
            if (region != null && !region.getId().isBlank()) {
                regions.add(region);
            }
        }
        return regions;
    }

    static void save(@Nonnull Path file, @Nonnull List<FishingSpawnRegion> regions) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        JsonArray regionsArray = new JsonArray();
        for (FishingSpawnRegion region : regions) {
            regionsArray.add(toJson(region));
        }
        root.add("Regions", regionsArray);
        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    @Nullable
    private static FishingSpawnRegion parseRegion(@Nonnull JsonObject object) {
        FishingSpawnRegion region = new FishingSpawnRegion();
        if (object.has("Id")) {
            region.setId(object.get("Id").getAsString());
        }
        if (object.has("Name") && !object.get("Name").isJsonNull()) {
            region.setName(object.get("Name").getAsString());
        }
        int[] min = readIntTriple(object, "Min");
        int[] max = readIntTriple(object, "Max");
        if (min == null || max == null) {
            return null;
        }
        region.setMinX(min[0]);
        region.setMinY(min[1]);
        region.setMinZ(min[2]);
        region.setMaxX(max[0]);
        region.setMaxY(max[1]);
        region.setMaxZ(max[2]);
        if (object.has("WaterBodyType") && !object.get("WaterBodyType").isJsonNull()) {
            region.setWaterBodyType(object.get("WaterBodyType").getAsString());
        }
        if (object.has("Environments") && object.get("Environments").isJsonArray()) {
            JsonArray envArray = object.getAsJsonArray("Environments");
            String[] envs = new String[envArray.size()];
            for (int i = 0; i < envArray.size(); i++) {
                envs[i] = envArray.get(i).getAsString();
            }
            region.setEnvironments(envs);
        }
        if (object.has("ZoneOverride") && !object.get("ZoneOverride").isJsonNull()) {
            region.setZoneOverride(object.get("ZoneOverride").getAsString());
        }
        if (object.has("VirtualBiome") && !object.get("VirtualBiome").isJsonNull()) {
            region.setVirtualBiome(object.get("VirtualBiome").getAsString());
        }
        if (object.has("IgnoreWorldZoneGate")) {
            region.setIgnoreWorldZoneGate(object.get("IgnoreWorldZoneGate").getAsBoolean());
        }
        return region;
    }

    @Nonnull
    private static JsonObject toJson(@Nonnull FishingSpawnRegion region) {
        JsonObject object = new JsonObject();
        object.addProperty("Id", region.getId());
        if (region.getName() != null) {
            object.addProperty("Name", region.getName());
        }
        object.add("Min", toIntTriple(region.getMinX(), region.getMinY(), region.getMinZ()));
        object.add("Max", toIntTriple(region.getMaxX(), region.getMaxY(), region.getMaxZ()));
        if (region.getWaterBodyType() != null) {
            object.addProperty("WaterBodyType", region.getWaterBodyType());
        }
        if (region.getEnvironments() != null && region.getEnvironments().length > 0) {
            JsonArray envArray = new JsonArray();
            for (String env : region.getEnvironments()) {
                envArray.add(env);
            }
            object.add("Environments", envArray);
        }
        if (region.getZoneOverride() != null) {
            object.addProperty("ZoneOverride", region.getZoneOverride());
        }
        if (region.getVirtualBiome() != null) {
            object.addProperty("VirtualBiome", region.getVirtualBiome());
        }
        object.addProperty("IgnoreWorldZoneGate", region.isIgnoreWorldZoneGate());
        return object;
    }

    @Nullable
    private static int[] readIntTriple(@Nonnull JsonObject object, @Nonnull String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return null;
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() < 3) {
            return null;
        }
        return new int[] { array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt() };
    }

    @Nonnull
    private static JsonArray toIntTriple(int x, int y, int z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
