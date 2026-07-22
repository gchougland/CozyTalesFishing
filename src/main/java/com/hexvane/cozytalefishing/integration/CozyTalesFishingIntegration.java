package com.hexvane.cozytalefishing.integration;

import com.hexvane.cozytalefishing.fish.FishRarity;
import com.hexvane.cozytalefishing.fish.FishableFluidRegistry;
import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.FishSpeciesDisplayNames;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Stable hook surface for optional integrations (e.g. MMO Skill Tree). Safe when no listeners are registered.
 */
public final class CozyTalesFishingIntegration {
    private static final List<Consumer<FishCaughtEvent>> FISH_CAUGHT_LISTENERS = new CopyOnWriteArrayList<>();

    private CozyTalesFishingIntegration() {}

    /** Resolves optional integrations (e.g. MMO Skill Tree). Safe to call more than once. */
    public static void setupIntegrations() {
        MMOSkillTreeIntegration.setup();
    }

    public record FishCaughtEvent(
        @Nonnull UUID playerUuid,
        @Nonnull String playerName,
        @Nonnull String speciesId,
        @Nonnull String fishItemId,
        int rarityOrdinal,
        float sizeCm,
        boolean newSpecies
    ) {}

    public static void addFishCaughtListener(@Nonnull Consumer<FishCaughtEvent> listener) {
        FISH_CAUGHT_LISTENERS.add(listener);
    }

    public static void removeFishCaughtListener(@Nonnull Consumer<FishCaughtEvent> listener) {
        FISH_CAUGHT_LISTENERS.remove(listener);
    }

    /**
     * Registers a mod fluid as fishable at runtime (merged with {@code CozyTalesFishing/Config/FishableFluids} assets).
     */
    public static void registerFishableFluid(
        @Nonnull String fluidAssetId,
        @Nonnull String habitatId,
        @Nullable String journalHabitatLangKey
    ) {
        FishableFluidRegistry.register(fluidAssetId, habitatId, journalHabitatLangKey);
    }

    public static void unregisterFishableFluid(@Nonnull String fluidAssetId) {
        FishableFluidRegistry.unregister(fluidAssetId);
    }

    public static boolean isFishItem(@Nullable String itemIdentifier) {
        if (itemIdentifier == null || itemIdentifier.isBlank()) {
            return false;
        }
        FishSpeciesAsset species = FishSpeciesRegistry.getSpeciesByItemId(itemIdentifier);
        return species != null && isJournalFish(species);
    }

    @Nullable
    public static String getFishDisplayName(@Nullable String fishIdentifier) {
        if (fishIdentifier == null || fishIdentifier.isBlank()) {
            return null;
        }
        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(fishIdentifier);
        if (species == null) {
            species = FishSpeciesRegistry.getSpeciesByItemId(fishIdentifier);
        }
        if (species == null || !isJournalFish(species)) {
            return null;
        }
        return FishSpeciesDisplayNames.resolve(species);
    }

    public static int getRarityOrdinal(@Nullable String fishIdentifier) {
        FishSpeciesAsset species = resolveJournalSpecies(fishIdentifier);
        return species != null ? species.getRarity().ordinal() : 0;
    }

    public static void notifyJournalFishCaught(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef playerRefComponent,
        @Nonnull FishSpeciesAsset species,
        float sizeCm,
        boolean newSpecies
    ) {
        if (!isJournalFish(species)) {
            return;
        }
        FishCaughtEvent event =
            new FishCaughtEvent(
                playerRefComponent.getUuid(),
                playerRefComponent.getUsername(),
                species.getId(),
                species.getItemId(),
                species.getRarity().ordinal(),
                sizeCm,
                newSpecies
            );
        for (Consumer<FishCaughtEvent> listener : FISH_CAUGHT_LISTENERS) {
            try {
                listener.accept(event);
            } catch (RuntimeException ignored) {
                // Optional integrations must not break catches.
            }
        }
        MMOSkillTreeIntegration.handleFishCaught(commandBuffer, playerRef, playerRefComponent, event);
    }

    @Nullable
    private static FishSpeciesAsset resolveJournalSpecies(@Nullable String fishIdentifier) {
        if (fishIdentifier == null || fishIdentifier.isBlank()) {
            return null;
        }
        FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(fishIdentifier);
        if (species == null) {
            species = FishSpeciesRegistry.getSpeciesByItemId(fishIdentifier);
        }
        if (species == null || !isJournalFish(species)) {
            return null;
        }
        return species;
    }

    private static boolean isJournalFish(@Nonnull FishSpeciesAsset species) {
        return !species.isTrash() && !species.isTreasure() && !species.isMonster() && !species.excludesFromJournal();
    }

    static double defaultXpForRarity(@Nonnull FishRarity rarity) {
        return switch (rarity) {
            case Common -> 10.0;
            case Uncommon -> 12.0;
            case Rare -> 18.0;
            case Epic -> 35.0;
            case Legendary -> 80.0;
        };
    }
}
