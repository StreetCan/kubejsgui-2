package com.streetkube.kubejsgui.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans the server's {@link RecipeManager} for the set of recipe type IDs currently
 * loaded (vanilla + datapacks + mods), and for the full recipe list used by the Modify
 * tab's matching-recipes preview.
 */
public final class RecipeScanner {

    private RecipeScanner() {
    }

    public static Set<String> getLoadedRecipeTypes(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        Set<String> types = new LinkedHashSet<>();

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (typeId != null) {
                types.add(typeId.toString());
            }
        }

        return types;
    }

    /** A flattened recipe summary: id, type, output item id, and the set of input item ids. */
    public record RecipeInfo(String id, String type, String output, List<String> inputs) {
    }

    public static List<RecipeInfo> getAllRecipes(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        List<RecipeInfo> out = new ArrayList<>();

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            try {
                ResourceLocation id = holder.id();
                ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
                String type = typeId != null ? typeId.toString() : "unknown";

                String output = "";
                try {
                    ItemStack result = holder.value().getResultItem(server.registryAccess());
                    if (result != null && !result.isEmpty()) {
                        ResourceLocation outId = BuiltInRegistries.ITEM.getKey(result.getItem());
                        output = outId != null ? outId.toString() : "";
                    }
                } catch (RuntimeException ignored) {
                    // Special/dynamic recipes have no static result item.
                }

                Set<String> inputs = new LinkedHashSet<>();
                try {
                    for (Ingredient ingredient : holder.value().getIngredients()) {
                        if (ingredient == null) {
                            continue;
                        }
                        for (ItemStack stack : ingredient.getItems()) {
                            ResourceLocation inId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (inId != null) {
                                inputs.add(inId.toString());
                            }
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Some recipe types don't expose ingredients in a readable form.
                }

                out.add(new RecipeInfo(id.toString(), type, output, new ArrayList<>(inputs)));
            } catch (RuntimeException ignored) {
                // Never let one malformed recipe abort the whole scan.
            }
        }

        return out;
    }
}
