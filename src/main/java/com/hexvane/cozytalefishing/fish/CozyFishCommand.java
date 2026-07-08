package com.hexvane.cozytalefishing.fish;

import com.hexvane.cozytalefishing.leaderboard.FishingLeaderboardService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.PLAYER_REF;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

/** Root admin command collection for Cozy Tales fishing tools. */
public final class CozyFishCommand extends AbstractCommandCollection {
    public CozyFishCommand() {
        super("cozyfish", "Cozy Tales fishing admin commands.");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        requirePermission(HytalePermissions.fromCommand("cozyfish"));
        addSubCommand(new FishingSpawnRegionCommand());
        addSubCommand(new FishingJournalCommand());
        addSubCommand(new ReloadConfigCommand());
    }

    static final class ReloadConfigCommand extends CommandBase {
        ReloadConfigCommand() {
            super("reloadconfig", "Reload FishingModConfig.json from disk.");
            requirePermission(HytalePermissions.fromCommand("cozyfish.reloadconfig"));
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            var plugin = com.hexvane.cozytalefishing.CozyTalesFishingPlugin.get();
            if (plugin == null) {
                context.sendMessage(Message.raw("CozyTalesFishing plugin is not loaded."));
                return;
            }
            plugin.reloadFishingModConfig();
            FishingModConfig config = FishingModConfig.get();
            context.sendMessage(
                Message.raw(
                    String.format(
                        Locale.US,
                        "Reloaded FishingModConfig.%n"
                            + "RodTipBob: amplitude=%.3f frequency=%.2f viewUpWeight=%.2f phaseOffset=%.3f reelAnimSpeed=%.2f seedWorldTime=%s%n"
                            + "RodTipAttach: horizontal=%.3f vertical=%.3f shaftScale=%.3f lift=%.3f",
                        config.getRodTipBobAmplitude(),
                        config.getRodTipBobFrequency(),
                        config.getRodTipBobViewUpWeight(),
                        config.getRodTipBobPhaseOffset(),
                        config.getRodTipBobReelAnimSpeed(),
                        config.isRodTipBobSeedFromWorldTime(),
                        config.getRodTipAttachHorizontal(),
                        config.getRodTipAttachVertical(),
                        config.getRodTipShaftLengthScale(),
                        config.getRodTipVerticalLift()
                    )
                )
            );
        }
    }

    static final class FishingJournalCommand extends AbstractCommandCollection {
        FishingJournalCommand() {
            super("journal", "Cozy Tales fishing journal admin commands.");
            requirePermission(HytalePermissions.fromCommand("cozyfish.journal"));
            addSubCommand(new JournalOpenCommand());
            addSubCommand(new JournalUnlockCommand());
            addSubCommand(new JournalHintCommand());
            addSubCommand(new JournalResetRecordsCommand());
        }
    }

    static final class JournalOpenCommand extends AbstractPlayerCommand {
        JournalOpenCommand() {
            super("open", "Open the fishing journal UI.");
            requirePermission(HytalePermissions.fromCommand("cozyfish.journal"));
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            if (player.getPageManager().getCustomPage() != null) {
                context.sendMessage(Message.translation("server.cozytalefishing.journal.command.page_busy"));
                return;
            }
            player.getPageManager().openCustomPage(ref, store, new com.hexvane.cozytalefishing.journal.FishingJournalPage(playerRef));
        }
    }

    static final class JournalUnlockCommand extends AbstractPlayerCommand {
        @Nonnull
        private final OptionalArg<String> speciesArg =
            withOptionalArg("speciesId", "Species id to unlock (e.g. CozyFish_Lobster). Omit to unlock all.", STRING);

        JournalUnlockCommand() {
            super("unlock", "Unlock fishing journal entries for testing.");
            requirePermission(HytalePermissions.fromCommand("cozyfish.journal"));
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
            if (records == null) {
                records = new FishCatchRecordComponent();
            }

            String speciesId = speciesArg.get(context);
            if (speciesId == null || speciesId.isBlank()) {
                List<FishSpeciesAsset> allSpecies = FishSpeciesRegistry.getAllSpecies();
                records.discoverAll(allSpecies.stream().map(FishSpeciesAsset::getId).toList());
                store.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
                context.sendMessage(
                    Message.translation("server.cozytalefishing.journal.command.unlocked_all")
                        .param("count", allSpecies.size())
                );
                return;
            }

            String trimmed = speciesId.trim();
            if (FishSpeciesRegistry.getSpecies(trimmed) == null) {
                context.sendMessage(Message.translation("server.cozytalefishing.journal.command.unknown_species").param("id", trimmed));
                return;
            }

            records.discover(trimmed);
            store.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
            context.sendMessage(Message.translation("server.cozytalefishing.journal.command.unlocked_one").param("id", trimmed));
        }
    }

    static final class JournalHintCommand extends AbstractPlayerCommand {
        @Nonnull
        private final OptionalArg<String> speciesArg =
            withOptionalArg("speciesId", "Species id to hint (e.g. CozyFish_Lobster). Omit to hint all undiscovered.", STRING);

        JournalHintCommand() {
            super("hint", "Hint fishing journal entries for testing.");
            requirePermission(HytalePermissions.fromCommand("cozyfish.journal"));
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
            if (records == null) {
                records = new FishCatchRecordComponent();
            }

            String speciesId = speciesArg.get(context);
            if (speciesId == null || speciesId.isBlank()) {
                List<String> undiscovered = new ArrayList<>();
                for (FishSpeciesAsset species : FishSpeciesRegistry.getJournalSpecies()) {
                    if (!records.isDiscovered(species.getId())) {
                        undiscovered.add(species.getId());
                    }
                }
                records.hintAll(undiscovered);
                store.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
                context.sendMessage(
                    Message.translation("server.cozytalefishing.journal.command.hinted_all")
                        .param("count", undiscovered.size())
                );
                return;
            }

            String trimmed = speciesId.trim();
            if (FishSpeciesRegistry.getSpecies(trimmed) == null) {
                context.sendMessage(Message.translation("server.cozytalefishing.journal.command.unknown_species").param("id", trimmed));
                return;
            }

            records.hint(trimmed);
            store.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
            context.sendMessage(Message.translation("server.cozytalefishing.journal.command.hinted_one").param("id", trimmed));
        }
    }

    static final class JournalResetRecordsCommand extends AbstractAsyncCommand {
        @Nonnull
        private final OptionalArg<PlayerRef> playerArg =
            withOptionalArg("player", "Player to reset (omit to reset all online players).", PLAYER_REF);

        JournalResetRecordsCommand() {
            super("resetrecords", "Reset fishing journal catch records.");
            requirePermission(HytalePermissions.fromCommand("cozyfish.journal.resetrecords"));
        }

        @Override
        protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            if (playerArg.provided(context)) {
                PlayerRef target = playerArg.get(context);
                Ref<EntityStore> ref = target.getReference();
                if (ref == null || !ref.isValid()) {
                    context.sendMessage(Message.translation("server.cozytalefishing.journal.command.player_not_in_world"));
                    return CompletableFuture.completedFuture(null);
                }
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return runAsync(
                    context,
                    () -> {
                        resetCatchRecords(store, ref);
                        context.sendMessage(
                            Message.translation("server.cozytalefishing.journal.command.reset_one")
                                .param("player", target.getUsername())
                        );
                    },
                    world
                );
            }

            Collection<PlayerRef> players = Universe.get().getPlayers();
            if (players.isEmpty()) {
                context.sendMessage(Message.translation("server.cozytalefishing.journal.command.reset_none_online"));
                return CompletableFuture.completedFuture(null);
            }

            Map<World, List<Ref<EntityStore>>> byWorld = new HashMap<>();
            for (PlayerRef playerRef : players) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    continue;
                }
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                byWorld.computeIfAbsent(world, ignored -> new ArrayList<>()).add(ref);
            }

            if (byWorld.isEmpty()) {
                context.sendMessage(Message.translation("server.cozytalefishing.journal.command.reset_none_online"));
                return CompletableFuture.completedFuture(null);
            }

            AtomicInteger resetCount = new AtomicInteger();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Map.Entry<World, List<Ref<EntityStore>>> entry : byWorld.entrySet()) {
                World world = entry.getKey();
                List<Ref<EntityStore>> refs = entry.getValue();
                futures.add(
                    runAsync(
                        context,
                        () -> {
                            Store<EntityStore> store = world.getEntityStore().getStore();
                            for (Ref<EntityStore> ref : refs) {
                                resetCatchRecords(store, ref);
                                resetCount.incrementAndGet();
                            }
                        },
                        world
                    )
                );
            }

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRun(
                    () ->
                        context.sendMessage(
                            Message.translation("server.cozytalefishing.journal.command.reset_all")
                                .param("count", resetCount.get())
                        )
                );
        }
    }

    private static void resetCatchRecords(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
        if (records == null) {
            store.putComponent(ref, FishCatchRecordComponent.getComponentType(), new FishCatchRecordComponent());
            return;
        }
        records.clear();
        store.putComponent(ref, FishCatchRecordComponent.getComponentType(), records);
        FishingLeaderboardService.invalidate();
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

            int envIndex = resolveEnvironmentIndex(world, blockX, surfaceY, blockZ);

            WaterBodyClassifier.Context classifyContext =
                new WaterBodyClassifier.Context(FishingModConfig.get().getMaxFloodFillsPerSpawnCheck());
            WaterBodyType classified =
                WaterBodyClassifier.classify(world, blockX, surfaceY, blockZ, classifyContext, envIndex);
            WaterBodyType effective = classified != null
                ? classified
                : (WaterBodyClassifier.isOceanEnvironmentForSpawn(envIndex) ? WaterBodyType.Ocean : WaterBodyType.Pond);
            if (regionContext != null && regionContext.getWaterBodyOverride() != null) {
                effective = regionContext.getWaterBodyOverride();
            }

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
