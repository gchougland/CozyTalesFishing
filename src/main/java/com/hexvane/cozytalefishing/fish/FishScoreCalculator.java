package com.hexvane.cozytalefishing.fish;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishScoreCalculator {
    private FishScoreCalculator() {}

    public static int scoreCatch(@Nonnull FishSpeciesAsset species, float sizeCm) {
        if (species.excludesFromJournal() || sizeCm <= 0.0f) {
            return 0;
        }
        float maxSizeCm = maxSizeCm(species.getSizeRangeCm());
        if (maxSizeCm <= 0.0f) {
            return 0;
        }
        return Math.round(rarityBaseScore(species.getRarity()) * (sizeCm / maxSizeCm));
    }

    public static int totalScore(@Nullable FishCatchRecordComponent records) {
        if (records == null) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<String, Float> entry : records.getLargestSizesBySpecies().entrySet()) {
            FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(entry.getKey());
            if (species == null) {
                continue;
            }
            total += scoreCatch(species, entry.getValue());
        }
        return total;
    }

    public static int bestCatchScore(@Nullable FishCatchRecordComponent records) {
        if (records == null) {
            return 0;
        }
        int best = 0;
        for (Map.Entry<String, Float> entry : records.getLargestSizesBySpecies().entrySet()) {
            FishSpeciesAsset species = FishSpeciesRegistry.getSpecies(entry.getKey());
            if (species == null) {
                continue;
            }
            best = Math.max(best, scoreCatch(species, entry.getValue()));
        }
        return best;
    }

    public static int rarityBaseScore(@Nonnull FishRarity rarity) {
        return switch (rarity) {
            case Common -> 100;
            case Uncommon -> 250;
            case Rare -> 500;
            case Epic -> 1_000;
            case Legendary -> 2_000;
        };
    }

    private static float maxSizeCm(@Nonnull float[] sizeRangeCm) {
        if (sizeRangeCm.length == 0) {
            return 0.0f;
        }
        float max = sizeRangeCm[0];
        for (int i = 1; i < sizeRangeCm.length; i++) {
            max = Math.max(max, sizeRangeCm[i]);
        }
        return max;
    }
}
