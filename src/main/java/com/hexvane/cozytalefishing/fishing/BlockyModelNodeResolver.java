package com.hexvane.cozytalefishing.fishing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Resolves named node positions from {@code .blockymodel} files (model-local block space).
 * Mirrors {@code BlockyModelBoundsParser} transform accumulation.
 */
public final class BlockyModelNodeResolver {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BLOCK_SCALE = 1f / 32f;

    private BlockyModelNodeResolver() {}

    /**
     * @return world position of the named node in model-local block coordinates, or {@code null} if missing.
     */
    @Nullable
    public static Vector3d findNodePositionBlocks(@Nonnull String modelPath, @Nonnull String nodeName) {
        CommonAsset asset = CommonAssetRegistry.getByName(modelPath);
        if (asset == null) {
            return null;
        }
        try {
            byte[] bytes = asset.getBlob().join();
            if (bytes == null) {
                return null;
            }
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray nodes = root.getAsJsonArray("nodes");
            if (nodes == null) {
                return null;
            }
            Vector3f found = new Vector3f();
            for (var nodeElement : nodes) {
                if (!nodeElement.isJsonObject()) {
                    continue;
                }
                if (findNode(nodeElement.getAsJsonObject(), nodeName, new Vector3f(), new Quaternionf(), found)) {
                    return new Vector3d(found.x * BLOCK_SCALE, found.y * BLOCK_SCALE, found.z * BLOCK_SCALE);
                }
            }
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).withCause(ex).log("Failed to resolve node '%s' in blockymodel: %s", nodeName, modelPath);
        }
        return null;
    }

    private static boolean findNode(
        @Nonnull JsonObject node,
        @Nonnull String targetName,
        @Nonnull Vector3f parentPosition,
        @Nonnull Quaternionf parentOrientation,
        @Nonnull Vector3f outWorldPosition
    ) {
        Vector3f worldPosition = transformNode(node, parentPosition, parentOrientation);
        Quaternionf worldOrientation = worldOrientation(node, parentOrientation);

        String name = node.has("name") ? node.get("name").getAsString() : "";
        if (targetName.equalsIgnoreCase(name)) {
            outWorldPosition.set(worldPosition);
            return true;
        }

        JsonArray children = node.getAsJsonArray("children");
        if (children != null) {
            for (var childElement : children) {
                if (!childElement.isJsonObject()) {
                    continue;
                }
                if (findNode(childElement.getAsJsonObject(), targetName, worldPosition, worldOrientation, outWorldPosition)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nonnull
    private static Vector3f transformNode(
        @Nonnull JsonObject node,
        @Nonnull Vector3f parentPosition,
        @Nonnull Quaternionf parentOrientation
    ) {
        JsonObject shape = node.getAsJsonObject("shape");
        Vector3f position = readVec3(node.getAsJsonObject("position"), 0, 0, 0);
        Quaternionf orientation = readQuat(node.getAsJsonObject("orientation"));
        Vector3f offset = shape != null ? readVec3(shape.getAsJsonObject("offset"), 0, 0, 0) : new Vector3f();

        Vector3f localPosition = new Vector3f(offset);
        localPosition.rotate(orientation);
        localPosition.add(position);

        Vector3f worldPosition = new Vector3f(localPosition);
        worldPosition.rotate(parentOrientation);
        worldPosition.add(parentPosition);
        return worldPosition;
    }

    @Nonnull
    private static Quaternionf worldOrientation(@Nonnull JsonObject node, @Nonnull Quaternionf parentOrientation) {
        Quaternionf orientation = readQuat(node.getAsJsonObject("orientation"));
        Quaternionf worldOrientation = new Quaternionf(parentOrientation);
        worldOrientation.mul(orientation);
        return worldOrientation;
    }

    @Nonnull
    private static Vector3f readVec3(@Nullable JsonObject obj, float defX, float defY, float defZ) {
        if (obj == null) {
            return new Vector3f(defX, defY, defZ);
        }
        float x = obj.has("x") ? obj.get("x").getAsFloat() : defX;
        float y = obj.has("y") ? obj.get("y").getAsFloat() : defY;
        float z = obj.has("z") ? obj.get("z").getAsFloat() : defZ;
        return new Vector3f(x, y, z);
    }

    @Nonnull
    private static Quaternionf readQuat(@Nullable JsonObject obj) {
        if (obj == null) {
            return new Quaternionf();
        }
        float x = obj.has("x") ? obj.get("x").getAsFloat() : 0;
        float y = obj.has("y") ? obj.get("y").getAsFloat() : 0;
        float z = obj.has("z") ? obj.get("z").getAsFloat() : 0;
        float w = obj.has("w") ? obj.get("w").getAsFloat() : 1;
        return new Quaternionf(x, y, z, w);
    }
}
