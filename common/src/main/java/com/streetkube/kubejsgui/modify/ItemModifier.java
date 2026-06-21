package com.streetkube.kubejsgui.modify;

/**
 * Generates the client and/or startup scripts for the Modify tab's item operations.
 * A single Generate action may produce up to two files (client + startup), so each
 * builder returns {@code null} when its operation group is unused.
 */
public final class ItemModifier {

    private ItemModifier() {
    }

    /** Client script: hide the item from JEI/REI. Returns null when not requested. */
    public static String generateClient(String itemId, boolean hideFromJei) {
        if (!hideFromJei) {
            return null;
        }
        return "ClientEvents.hideClientRecipes(event => {\n"
                + "  event.hide('" + esc(itemId) + "')\n"
                + "})\n";
    }

    /**
     * Startup script combining the ban-item tag add and any property modifications.
     * Returns null when neither is requested.
     */
    public static String generateStartup(String itemId, boolean banItem,
                                         Integer maxDamage, Integer maxStackSize, String displayName) {
        boolean hasProps = maxDamage != null || maxStackSize != null
                || (displayName != null && !displayName.isBlank());
        if (!banItem && !hasProps) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (banItem) {
            sb.append("StartupEvents.tags('item', event => {\n")
              .append("  event.add('kubejs:banned_items', '").append(esc(itemId)).append("')\n")
              .append("})\n");
        }
        if (hasProps) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("StartupEvents.modifyItemProperties(event => {\n")
              .append("  event.modify('").append(esc(itemId)).append("', item => {\n");
            if (maxDamage != null) {
                sb.append("    item.maxDamage = ").append(maxDamage).append('\n');
            }
            if (maxStackSize != null) {
                sb.append("    item.maxStackSize = ").append(maxStackSize).append('\n');
            }
            if (displayName != null && !displayName.isBlank()) {
                sb.append("    item.displayName = '").append(esc(displayName)).append("'\n");
            }
            sb.append("  })\n})\n");
        }
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
