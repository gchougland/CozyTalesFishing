package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

/** Root admin command collection for Cozy Tales fishing tools. */
public final class CozyFishCommand extends AbstractCommandCollection {
    public CozyFishCommand() {
        super("cozyfish", "Cozy Tales fishing admin commands.");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        requirePermission(HytalePermissions.fromCommand("cozyfish"));
        addSubCommand(new FishingSpawnRegionCommand());
    }

    static final class FishingSpawnRegionCommand extends AbstractCommandCollection {
        FishingSpawnRegionCommand() {
            super("region", "Manage fishing spawn regions for custom worlds.");
            addSubCommand(new RegionCreateCommand());
            addSubCommand(new RegionUpdateCommand());
            addSubCommand(new RegionRemoveCommand());
            addSubCommand(new RegionListCommand());
            addSubCommand(new RegionProbeCommand());
        }
    }

    abstract static class RegionWriteCommand extends AbstractPlayerCommand {
        @Nonnull
        protected final RequiredArg<String> idArg =
            withRequiredArg("id", "Unique region id (e.g. main_ocean).", STRING);
        @Nonnull
        protected final OptionalArg<String> waterArg =
            withOptionalArg("water", "Force water body: Pond, River, or Ocean.", STRING);
        @Nonnull
        protected final OptionalArg<List<String>> envArg =
            withListOptionalArg("env", "Allowed environment id(s), e.g. Env_Zone0_Warm.", STRING);
        @Nonnull
        protected final OptionalArg<String> zoneArg =
            withOptionalArg("zone", "World zone override, e.g. Zone0 or Zone3.", STRING);
        @Nonnull
        protected final OptionalArg<String> biomeArg =
            withOptionalArg("biome", "Virtual biome name for species SpawnLocation.Biomes matching.", STRING);
        @Nonnull
        protected final OptionalArg<String> nameArg =
            withOptionalArg("name", "Display name for the region.", STRING);
        @Nonnull
        protected final FlagArg respectWorldZoneArg =
            withFlagArg("respectWorldZone", "Do not ignore the worldgen zone gate inside this region.");

        RegionWriteCommand(@Nonnull String name, @Nonnull String description) {
            super(name, description);
            requirePermission(HytalePermissions.fromCommand("cozyfish.region"));
        }

        @Nullable
        protected FishingSpawnRegionSelectionUtil.Bounds requireSelection(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Player component missing."));
                return null;
            }
            FishingSpawnRegionSelectionUtil.Bounds bounds = FishingSpawnRegionSelectionUtil.readBuilderSelection(player, playerRef);
            if (bounds == null) {
                context.sendMessage(
                    Message.raw("No builder selection — select a volume with Builder Tools first (same as /environment).")
                );
            }
            return bounds;
        }

        protected void applyOptionalOverrides(@Nonnull CommandContext context, @Nonnull FishingSpawnRegion region) {
            if (waterArg.provided(context)) {
                WaterBodyType bodyType = WaterBodyType.fromString(waterArg.get(context));
                if (bodyType == null) {
                    throw new IllegalArgumentException("Invalid --water value. Use Pond, River, or Ocean.");
                }
                region.setWaterBodyType(bodyType.name());
            }
            if (envArg.provided(context)) {
                List<String> envs = envArg.get(context);
                region.setEnvironments(envs.toArray(String[]::new));
            }
            if (zoneArg.provided(context)) {
                region.setZoneOverride(zoneArg.get(context));
            }
            if (biomeArg.provided(context)) {
                region.setVirtualBiome(biomeArg.get(context));
            }
            if (nameArg.provided(context)) {
                region.setName(nameArg.get(context));
            }
            if (respectWorldZoneArg.provided(context)) {
                region.setIgnoreWorldZoneGate(false);
            }
        }

        protected void bindBounds(@Nonnull FishingSpawnRegion region, @Nonnull FishingSpawnRegionSelectionUtil.Bounds bounds) {
            region.setMinX(bounds.minX());
            region.setMinY(bounds.minY());
            region.setMinZ(bounds.minZ());
            region.setMaxX(bounds.maxX());
            region.setMaxY(bounds.maxY());
            region.setMaxZ(bounds.maxZ());
        }

        protected void sendRegionSaved(@Nonnull CommandContext context, @Nonnull FishingSpawnRegion region, @Nonnull String action) {
            context.sendMessage(
                Message.raw(
                    String.format(
                        Locale.US,
                        "%s spawn region '%s' [%d,%d,%d] -> [%d,%d,%d] water=%s zone=%s env=%s",
                        action,
                        region.getId(),
                        region.getMinX(),
                        region.getMinY(),
                        region.getMinZ(),
                        region.getMaxX(),
                        region.getMaxY(),
                        region.getMaxZ(),
                        region.getWaterBodyType() != null ? region.getWaterBodyType() : "auto",
                        region.getZoneOverride() != null ? region.getZoneOverride() : "none",
                        region.getEnvironments() != null ? Arrays.toString(region.getEnvironments()) : "none"
                    )
                )
            );
        }
    }

    static final class RegionCreateCommand extends RegionWriteCommand {
        RegionCreateCommand() {
            super("create", "Create a spawn region from the current builder selection.");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            FishingSpawnRegionSelectionUtil.Bounds bounds = requireSelection(context, store, ref, playerRef);
            if (bounds == null) {
                return;
            }

            String id = idArg.get(context);
            UUID worldUuid = world.getWorldConfig().getUuid();
            if (FishingSpawnRegionRegistry.getRegion(worldUuid, id) != null) {
                context.sendMessage(Message.raw("Region '" + id + "' already exists. Use /cozyfish region update instead."));
                return;
            }

            FishingSpawnRegion region = new FishingSpawnRegion();
            region.setId(id);
            bindBounds(region, bounds);
            try {
                applyOptionalOverrides(context, region);
            } catch (IllegalArgumentException e) {
                context.sendMessage(Message.raw(e.getMessage()));
                return;
            }

            if (!FishingSpawnRegionRegistry.upsert(worldUuid, region)) {
                context.sendMessage(Message.raw("Failed to save region '" + id + "'."));
                return;
            }
            sendRegionSaved(context, region, "Created");
        }
    }

    static final class RegionUpdateCommand extends RegionWriteCommand {
        RegionUpdateCommand() {
            super("update", "Update an existing spawn region (re-bind selection when provided).");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            String id = idArg.get(context);
            UUID worldUuid = world.getWorldConfig().getUuid();
            FishingSpawnRegion existing = FishingSpawnRegionRegistry.getRegion(worldUuid, id);
            if (existing == null) {
                context.sendMessage(Message.raw("Region '" + id + "' not found."));
                return;
            }

            FishingSpawnRegion region = existing.copy();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                FishingSpawnRegionSelectionUtil.Bounds bounds =
                    FishingSpawnRegionSelectionUtil.readBuilderSelection(player, playerRef);
                if (bounds != null) {
                    bindBounds(region, bounds);
                }
            }

            try {
                applyOptionalOverrides(context, region);
            } catch (IllegalArgumentException e) {
                context.sendMessage(Message.raw(e.getMessage()));
                return;
            }

            if (!FishingSpawnRegionRegistry.upsert(worldUuid, region)) {
                context.sendMessage(Message.raw("Failed to save region '" + id + "'."));
                return;
            }
            sendRegionSaved(context, region, "Updated");
        }
    }

    static final class RegionRemoveCommand extends AbstractPlayerCommand {
        @Nonnull
        private final RequiredArg<String> idArg =
            withRequiredArg("id", "Region id to remove.", STRING);

        RegionRemoveCommand() {
            super("remove", "Remove a spawn region.");
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
            requirePermission(HytalePermissions.fromCommand("cozyfish.region"));
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            String id = idArg.get(context);
            UUID worldUuid = world.getWorldConfig().getUuid();
            if (!FishingSpawnRegionRegistry.remove(worldUuid, id)) {
                context.sendMessage(Message.raw("Region '" + id + "' not found."));
                return;
            }
            context.sendMessage(Message.raw("Removed spawn region '" + id + "'."));
        }
    }

    static final class RegionListCommand extends AbstractPlayerCommand {
        RegionListCommand() {
            super("list", "List spawn regions in the current world.");
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
            requirePermission(HytalePermissions.fromCommand("cozyfish.region"));
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            List<FishingSpawnRegion> regions = FishingSpawnRegionRegistry.getRegions(world.getWorldConfig().getUuid());
            if (regions.isEmpty()) {
                context.sendMessage(Message.raw("No spawn regions defined for this world."));
                return;
            }
            StringBuilder text = new StringBuilder("Spawn regions (").append(regions.size()).append("):\n");
            for (FishingSpawnRegion region : regions) {
                text.append("  ")
                    .append(region.getId())
                    .append(" [")
                    .append(region.getMinX())
                    .append(',')
                    .append(region.getMinY())
                    .append(',')
                    .append(region.getMinZ())
                    .append("] -> [")
                    .append(region.getMaxX())
                    .append(',')
                    .append(region.getMaxY())
                    .append(',')
                    .append(region.getMaxZ())
                    .append("] water=")
                    .append(region.getWaterBodyType() != null ? region.getWaterBodyType() : "auto")
                    .append(" zone=")
                    .append(region.getZoneOverride() != null ? region.getZoneOverride() : "none")
                    .append('\n');
            }
            context.sendMessage(Message.raw(text.toString()));
        }
    }

    static final class RegionProbeCommand extends AbstractPlayerCommand {
        RegionProbeCommand() {
            super("probe", "Show active spawn region and effective overrides at your feet.");
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
            requirePermission(HytalePermissions.fromCommand("cozyfish.region"));
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                context.sendMessage(Message.raw("Could not read player position."));
                return;
            }

            Vector3d pos = transform.getPosition();
            int blockX = (int) Math.floor(pos.x);
            int blockZ = (int) Math.floor(pos.z);
            int surfaceY = FishShadowSpawnHelper.findSurfaceWaterBlockY(world, blockX, blockZ);
            if (surfaceY < 0) {
                context.sendMessage(Message.raw("No water at your feet — stand in water to probe spawn overrides."));
                return;
            }

            UUID worldUuid = world.getWorldConfig().getUuid();
            FishingSpawnRegionContext regionContext =
                FishingSpawnRegionRegistry.resolve(worldUuid, blockX, surfaceY, blockZ);

            WaterBodyClassifier.Context classifyContext =
                new WaterBodyClassifier.Context(FishingModConfig.get().getMaxFloodFillsPerSpawnCheck());
            WaterBodyType classified =
                WaterBodyClassifier.classify(world, blockX, surfaceY, blockZ, classifyContext);
            WaterBodyType effective = classified != null ? classified : WaterBodyType.Pond;
            if (regionContext != null && regionContext.getWaterBodyOverride() != null) {
                effective = regionContext.getWaterBodyOverride();
            }

            int envIndex = resolveEnvironmentIndex(world, blockX, surfaceY, blockZ);
            Environment envAsset = Environment.getAssetMap().getAsset(envIndex);
            String envName = envAsset != null ? envAsset.getId() : null;
            String biome = WaterBodyClassifier.getBiomeName(world, blockX, blockZ);
            String worldZone = FishShadowSpawnHelper.getWorldZoneName(world, blockX, blockZ);

            StringBuilder text = new StringBuilder();
            text.append("Spawn probe @ (")
                .append(blockX)
                .append(", ")
                .append(surfaceY)
                .append(", ")
                .append(blockZ)
                .append(")\n");
            text.append("  EnableSpawnRegions=").append(FishingModConfig.get().isEnableSpawnRegions()).append('\n');
            text.append("  classifiedWaterBody=").append(classified != null ? classified.name() : "unknown").append('\n');
            text.append("  effectiveWaterBody=").append(effective.name()).append('\n');
            text.append("  blockEnv=").append(envName != null ? envName : "unknown").append(" (index ").append(envIndex).append(")\n");
            text.append("  worldgenBiome=").append(biome != null ? biome : "unknown").append('\n');
            text.append("  worldgenZone=").append(worldZone != null ? worldZone : "unknown").append('\n');

            if (regionContext == null) {
                text.append("  spawnRegion=none\n");
            } else {
                FishingSpawnRegion region = regionContext.getRegion();
                text.append("  spawnRegion=").append(region.getId()).append('\n');
                if (region.getZoneOverride() != null) {
                    text.append("  zoneOverride=").append(region.getZoneOverride()).append('\n');
                }
                if (region.getEnvironments() != null && region.getEnvironments().length > 0) {
                    text.append("  regionEnvironments=").append(Arrays.toString(region.getEnvironments())).append('\n');
                }
                if (region.getVirtualBiome() != null) {
                    text.append("  virtualBiome=").append(region.getVirtualBiome()).append('\n');
                }
                text.append("  ignoreWorldZoneGate=").append(region.isIgnoreWorldZoneGate()).append('\n');
            }

            context.sendMessage(Message.raw(text.toString()));
        }

        private static int resolveEnvironmentIndex(@Nonnull World world, int blockX, int blockY, int blockZ) {
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(blockX, blockZ);
            var chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
            if (chunkRef == null) {
                return 0;
            }
            var worldChunk =
                world.getChunkStore().getStore().getComponent(chunkRef, com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk.getComponentType());
            if (worldChunk == null) {
                return 0;
            }
            return worldChunk.getBlockChunk().getEnvironment(blockX, blockY, blockZ);
        }
    }
}
