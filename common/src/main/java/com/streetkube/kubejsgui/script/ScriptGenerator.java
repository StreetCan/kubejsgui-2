package com.streetkube.kubejsgui.script;

import com.streetkube.kubejsgui.recipe.RecipeSchema;
import com.streetkube.kubejsgui.recipe.RecipeSchemas;
import com.streetkube.kubejsgui.server.ScriptRequest;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Turns a {@link ScriptRequest} into the body of a KubeJS server script, using a
 * switch on recipe type and plain {@link StringBuilder} templating.
 */
public final class ScriptGenerator {

    private ScriptGenerator() {
    }

    public static String generate(ScriptRequest request) {
        return switch (request.type()) {
            case "minecraft:crafting_shaped" -> shaped(request);
            case "minecraft:crafting_shapeless" -> shapeless(request);
            case "minecraft:smelting" -> cooking(request, "smelting");
            case "minecraft:blasting" -> cooking(request, "blasting");
            case "minecraft:smoking" -> cooking(request, "smoking");
            case "minecraft:stonecutting" -> stonecutting(request);
            case "create:mixing" -> createPool(request, "mixing");
            case "create:compacting" -> createPool(request, "compacting");
            case "create:pressing" -> createSingle(request, "pressing", false);
            case "create:cutting" -> createSingle(request, "cutting", true);
            case "create:filling" -> createFilling(request);
            default -> genericPool(request);
        };
    }

    private static String shaped(ScriptRequest req) {
        Map<String, String> slots = req.slots() != null ? req.slots() : Map.of();
        Map<String, Character> letters = new LinkedHashMap<>();
        char[] grid = new char[9];
        java.util.Arrays.fill(grid, ' ');

        char next = 'A';
        for (int i = 0; i < 9; i++) {
            String item = slots.get(String.valueOf(i));
            if (item == null) {
                continue;
            }
            Character letter = letters.get(item);
            if (letter == null) {
                letter = next++;
                letters.put(item, letter);
            }
            grid[i] = letter;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.shaped(\n");
        sb.append("    ").append(itemOf(req.output())).append(",\n");
        sb.append("    [\n");
        for (int row = 0; row < 3; row++) {
            String pattern = new String(grid, row * 3, 3);
            sb.append("      '").append(pattern).append('\'');
            sb.append(row < 2 ? ",\n" : "\n");
        }
        sb.append("    ],\n");
        sb.append("    {\n");
        int idx = 0;
        for (Map.Entry<String, Character> entry : letters.entrySet()) {
            sb.append("      ").append(entry.getValue()).append(": '").append(entry.getKey()).append('\'');
            sb.append(++idx < letters.size() ? ",\n" : "\n");
        }
        sb.append("    }\n");
        sb.append("  )\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static String shapeless(ScriptRequest req) {
        List<String> ingredients = orderedIngredients(req.slots());

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.shapeless(\n");
        sb.append("    ").append(itemOf(req.output())).append(",\n");
        sb.append("    [").append(quotedList(ingredients)).append("]\n");
        sb.append("  )\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static String cooking(ScriptRequest req, String method) {
        String input = singleInput(req.slots());
        String output = req.output() != null ? req.output().id() : "minecraft:air";

        Object xp = extraOrDefault(req, "experience");
        Object cookingTime = extraOrDefault(req, "cookingTime");

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.").append(method).append("('").append(output).append("', '").append(input).append("')\n");
        sb.append("    .xp(").append(xp).append(")\n");
        sb.append("    .cookingTime(").append(cookingTime).append(")\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static String stonecutting(ScriptRequest req) {
        String input = singleInput(req.slots());

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.stonecutting(").append(itemOf(req.output())).append(", '").append(input).append("')\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static String createPool(ScriptRequest req, String method) {
        List<String> ingredients = orderedIngredients(req.slots());
        Object heatLevel = extraOrDefault(req, "heatLevel");

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.recipes.create.").append(method).append("(\n");
        sb.append("    [").append(itemOf(req.output())).append("],\n");
        sb.append("    [").append(quotedList(ingredients)).append("]\n");
        sb.append("  )");
        if ("heated".equals(heatLevel)) {
            sb.append(".heated()");
        } else if ("superheated".equals(heatLevel)) {
            sb.append(".superheated()");
        }
        sb.append("\n})\n");
        return sb.toString();
    }

    private static String createSingle(ScriptRequest req, String method, boolean hasProcessingTime) {
        String input = singleInput(req.slots());

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.recipes.create.").append(method).append('(')
                .append(itemOf(req.output())).append(", '").append(input).append("')");

        if (hasProcessingTime) {
            Object time = extraOrDefault(req, "processingTime");
            sb.append("\n    .processingTime(").append(time).append(')');
        }

        sb.append("\n})\n");
        return sb.toString();
    }

    private static String createFilling(ScriptRequest req) {
        Map<String, String> slots = req.slots() != null ? req.slots() : Map.of();
        String slot0 = slots.getOrDefault("0", "minecraft:air");
        String slot1 = slots.getOrDefault("1", "minecraft:air");

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.recipes.create.filling(\n");
        sb.append("    ").append(itemOf(req.output())).append(",\n");
        sb.append("    ['").append(slot0).append("', '").append(slot1).append("']\n");
        sb.append("  )\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static String genericPool(ScriptRequest req) {
        List<String> ingredients = orderedIngredients(req.slots());

        StringBuilder sb = new StringBuilder();
        sb.append("// Unknown recipe type '").append(req.type()).append("' - generated as a generic shapeless recipe.\n");
        sb.append("// You may need to adjust this manually for the correct recipe event.\n");
        sb.append("ServerEvents.recipes(event => {\n");
        sb.append("  event.shapeless(\n");
        sb.append("    ").append(itemOf(req.output())).append(",\n");
        sb.append("    [").append(quotedList(ingredients)).append("]\n");
        sb.append("  )\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static String itemOf(ScriptRequest.OutputSpec output) {
        if (output == null || output.id() == null) {
            return "Item.of('minecraft:air', 1)";
        }
        return "Item.of('" + output.id() + "', " + output.count() + ")";
    }

    private static String singleInput(Map<String, String> slots) {
        if (slots == null) {
            return "minecraft:air";
        }
        return slots.getOrDefault("0", "minecraft:air");
    }

    private static List<String> orderedIngredients(Map<String, String> slots) {
        if (slots == null) {
            return List.of();
        }
        return slots.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String quotedList(List<String> items) {
        return items.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", "));
    }

    private static Object extraOrDefault(ScriptRequest req, String key) {
        Map<String, Object> extraData = req.extraData();
        if (extraData != null && extraData.containsKey(key)) {
            Object value = extraData.get(key);
            if (value instanceof Double d && d == Math.floor(d) && !key.equals("experience")) {
                return d.intValue();
            }
            return value;
        }

        for (RecipeSchema.ExtraField field : RecipeSchemas.get(req.type()).extraFields()) {
            if (field.key().equals(key)) {
                return field.defaultValue();
            }
        }
        return null;
    }
}
