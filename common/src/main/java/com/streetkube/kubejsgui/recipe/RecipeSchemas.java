package com.streetkube.kubejsgui.recipe;

import java.util.List;
import java.util.Map;

/**
 * Hardcoded slot-layout schemas for the recipe types known to the MVP. Unknown/modded
 * recipe types fall back to a generic "pool" layout with no extra fields.
 */
public final class RecipeSchemas {

    private static final Map<String, RecipeSchema> KNOWN = Map.ofEntries(
            Map.entry("minecraft:crafting_shaped", new RecipeSchema(
                    "minecraft:crafting_shaped", "Shaped Crafting", "grid", 3, 3, true, List.of())),
            Map.entry("minecraft:crafting_shapeless", new RecipeSchema(
                    "minecraft:crafting_shapeless", "Shapeless Crafting", "pool", null, null, true, List.of())),
            Map.entry("minecraft:smelting", new RecipeSchema(
                    "minecraft:smelting", "Smelting", "single", 1, 1, true, List.of(
                            new RecipeSchema.ExtraField("cookingTime", "int", 200),
                            new RecipeSchema.ExtraField("experience", "float", 0.1)))),
            Map.entry("minecraft:blasting", new RecipeSchema(
                    "minecraft:blasting", "Blasting", "single", 1, 1, true, List.of(
                            new RecipeSchema.ExtraField("cookingTime", "int", 100),
                            new RecipeSchema.ExtraField("experience", "float", 0.1)))),
            Map.entry("minecraft:smoking", new RecipeSchema(
                    "minecraft:smoking", "Smoking", "single", 1, 1, true, List.of(
                            new RecipeSchema.ExtraField("cookingTime", "int", 100),
                            new RecipeSchema.ExtraField("experience", "float", 0.1)))),
            Map.entry("minecraft:stonecutting", new RecipeSchema(
                    "minecraft:stonecutting", "Stonecutting", "single", 1, 1, true, List.of())),
            Map.entry("create:mixing", new RecipeSchema(
                    "create:mixing", "Mixing", "pool", null, null, true, List.of(
                            new RecipeSchema.ExtraField("heatLevel", "enum", "none",
                                    List.of("none", "heated", "superheated"))))),
            Map.entry("create:compacting", new RecipeSchema(
                    "create:compacting", "Compacting", "pool", null, null, true, List.of(
                            new RecipeSchema.ExtraField("heatLevel", "enum", "none",
                                    List.of("none", "heated", "superheated"))))),
            Map.entry("create:pressing", new RecipeSchema(
                    "create:pressing", "Pressing", "single", 1, 1, true, List.of())),
            Map.entry("create:cutting", new RecipeSchema(
                    "create:cutting", "Cutting", "single", 1, 1, true, List.of(
                            new RecipeSchema.ExtraField("processingTime", "int", 100)))),
            Map.entry("create:filling", new RecipeSchema(
                    "create:filling", "Filling", "pair", 1, 2, true, List.of()))
    );

    private RecipeSchemas() {
    }

    public static RecipeSchema get(String typeId) {
        RecipeSchema known = KNOWN.get(typeId);
        if (known != null) {
            return known;
        }
        return new RecipeSchema(typeId, typeId, "pool", null, null, true, List.of());
    }
}
