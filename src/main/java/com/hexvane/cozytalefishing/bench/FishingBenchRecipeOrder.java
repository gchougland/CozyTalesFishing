package com.hexvane.cozytalefishing.bench;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Orders Fishing Bench recipes for display.
 *
 * <p>Vanilla BasicCrafting has no recipe Order field — {@code CraftingPlugin} stores recipes in an
 * unordered set. Structural benches only sort by category index. The practical controls are:
 * <ul>
 *   <li>Server: ordered {@code craftableRecipes} array in window data (this class)</li>
 *   <li>Client (likely): ascending output {@code ItemLevel} — set on our craftable items</li>
 * </ul>
 *
 * <p>Our recipe ids are {@code CozyFishing_Bench_NN_...}; sort by that NN, then by id.
 */
public final class FishingBenchRecipeOrder {
    private static final Pattern BENCH_ORDER_PREFIX = Pattern.compile("^CozyFishing_Bench_(\\d+)_");

    private FishingBenchRecipeOrder() {}

    public static void sortRecipeIds(@Nonnull List<String> recipeIds) {
        recipeIds.sort(Comparator
            .comparingInt(FishingBenchRecipeOrder::benchOrderIndex)
            .thenComparing(id -> id));
    }

    private static int benchOrderIndex(@Nonnull String recipeId) {
        Matcher matcher = BENCH_ORDER_PREFIX.matcher(recipeId);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }
}
