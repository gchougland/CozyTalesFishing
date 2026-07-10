package com.hexvane.cozytalefishing.bench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.window.SimpleCraftingWindow;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Simple crafting window that applies {@link FishingBenchRecipeOrder} to each category. */
public final class FishingBenchCraftingWindow extends SimpleCraftingWindow {
    public FishingBenchCraftingWindow(
        int x,
        int y,
        int z,
        int rotationIndex,
        @Nonnull BlockType blockType,
        @Nonnull BenchBlock benchBlock
    ) {
        super(x, y, z, rotationIndex, blockType, benchBlock);
        sortCategoryRecipes();
    }

    private void sortCategoryRecipes() {
        JsonElement categoriesElement = getData().get("categories");
        if (categoriesElement == null || !categoriesElement.isJsonArray()) {
            return;
        }

        for (JsonElement categoryElement : categoriesElement.getAsJsonArray()) {
            if (!categoryElement.isJsonObject()) {
                continue;
            }
            JsonObject category = categoryElement.getAsJsonObject();
            JsonElement recipesElement = category.get("craftableRecipes");
            if (recipesElement == null || !recipesElement.isJsonArray()) {
                continue;
            }

            JsonArray recipesArray = recipesElement.getAsJsonArray();
            List<String> recipeIds = new ArrayList<>(recipesArray.size());
            for (JsonElement recipeElement : recipesArray) {
                if (recipeElement.isJsonPrimitive()) {
                    recipeIds.add(recipeElement.getAsString());
                }
            }

            FishingBenchRecipeOrder.sortRecipeIds(recipeIds);

            JsonArray sorted = new JsonArray();
            for (String recipeId : recipeIds) {
                sorted.add(recipeId);
            }
            category.add("craftableRecipes", sorted);
        }
    }
}
