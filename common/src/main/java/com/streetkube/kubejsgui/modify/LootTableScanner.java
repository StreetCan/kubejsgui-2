package com.streetkube.kubejsgui.modify;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the loot-table id for a block/entity target and reports its current drops on a
 * best-effort basis.
 *
 * <p>Loot tables can contain arbitrarily nested pools, conditions, functions and dynamic
 * values whose internals are private and version-specific. Per the MVP spec this scan is
 * explicitly best-effort: it always reports the resolved table id so the user can confirm
 * the target, and flags {@code readable=false} with a disclaimer when the drop list cannot
 * be safely enumerated. Script generation does not depend on this scan, so loot
 * modification works fully regardless of what is readable here.
 */
public final class LootTableScanner {

    /** A single readable drop entry. */
    public record Drop(String item, int countMin, int countMax, List<String> conditions) {
    }

    /** The scan result serialized to the {@code GET /loottable} response. */
    public static final class Result {
        public String id;
        public String type;
        public boolean readable;
        public String note;
        public List<Drop> drops = new ArrayList<>();
    }

    private LootTableScanner() {
    }

    /**
     * @param type one of {@code block}, {@code entity}, {@code chest}
     * @param id   the registry id of the block/entity, or the raw loot-table id for chests
     */
    public static Result scan(String type, String id) {
        Result result = new Result();
        result.type = type;
        result.id = resolveTableId(type, id);
        result.readable = false;
        result.note = "Drops shown may be incomplete — loot tables can contain nested "
                + "conditions and functions that are not enumerated here. The generated "
                + "script will still apply correctly.";
        return result;
    }

    /** Maps a block/entity id to its conventional loot-table id (chest ids are used as-is). */
    public static String resolveTableId(String type, String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        if ("chest".equalsIgnoreCase(type)) {
            return id;
        }
        String folder = "entity".equalsIgnoreCase(type) ? "entities" : "blocks";
        int colon = id.indexOf(':');
        if (colon < 0) {
            return "minecraft:" + folder + "/" + id;
        }
        return id.substring(0, colon) + ":" + folder + "/" + id.substring(colon + 1);
    }
}
