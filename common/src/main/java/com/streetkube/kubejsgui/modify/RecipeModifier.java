package com.streetkube.kubejsgui.modify;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates KubeJS {@code ServerEvents.recipes} scripts for the Modify tab's recipe
 * remove and replace operations. All scripts target {@code server_scripts/generated/}
 * and apply with {@code /kubejs reload}.
 */
public final class RecipeModifier {

    private RecipeModifier() {
    }

    /**
     * {@code event.remove({ ... })} including only the supplied non-blank filters
     * (keys: {@code type}, {@code output}, {@code input}, {@code id}). An empty filter
     * map produces {@code event.remove({})} which removes every recipe.
     */
    public static String generateRemove(Map<String, String> filters) {
        Map<String, String> clean = clean(filters);
        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");
        if (clean.isEmpty()) {
            sb.append("  event.remove({})\n");
        } else {
            sb.append("  event.remove({\n");
            int i = 0;
            for (Map.Entry<String, String> e : clean.entrySet()) {
                sb.append("    ").append(e.getKey()).append(": '").append(esc(e.getValue())).append('\'');
                sb.append(++i < clean.size() ? ",\n" : "\n");
            }
            sb.append("  })\n");
        }
        sb.append("})\n");
        return sb.toString();
    }

    public static String generateReplaceInput(Map<String, String> scopeFilters, String find, String replace) {
        return replace("replaceInput", scopeFilters, find, replace);
    }

    public static String generateReplaceOutput(Map<String, String> scopeFilters, String find, String replace) {
        return replace("replaceOutput", scopeFilters, find, replace);
    }

    private static String replace(String method, Map<String, String> scopeFilters, String find, String replace) {
        String scope = buildFilterObject(scopeFilters);
        return "ServerEvents.recipes(event => {\n"
                + "  event." + method + "(" + scope + ", '" + esc(find) + "', '" + esc(replace) + "')\n"
                + "})\n";
    }

    /** Inline filter object for replace operations: {@code {}} when empty, else {@code { k: 'v' }}. */
    private static String buildFilterObject(Map<String, String> filters) {
        Map<String, String> clean = clean(filters);
        if (clean.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{ ");
        int i = 0;
        for (Map.Entry<String, String> e : clean.entrySet()) {
            sb.append(e.getKey()).append(": '").append(esc(e.getValue())).append('\'');
            sb.append(++i < clean.size() ? ", " : " ");
        }
        sb.append('}');
        return sb.toString();
    }

    private static Map<String, String> clean(Map<String, String> filters) {
        Map<String, String> out = new LinkedHashMap<>();
        if (filters != null) {
            for (Map.Entry<String, String> e : filters.entrySet()) {
                if (e.getValue() != null && !e.getValue().isBlank()) {
                    out.put(e.getKey(), e.getValue().trim());
                }
            }
        }
        return out;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
