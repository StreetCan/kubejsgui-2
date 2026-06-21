package com.streetkube.kubejsgui.modify;

/**
 * Generates KubeJS {@code ServerEvents.lootTables} scripts for block and entity loot
 * modification. All scripts target {@code server_scripts/generated/} and apply with
 * {@code /kubejs reload}.
 */
public final class LootTableModifier {

    private LootTableModifier() {
    }

    public record Drop(String itemId, double chance, int countMin, int countMax) {
    }

    /** {@code targetType} is "block" or "entity"; selects modifyBlock / modifyEntity. */
    public static String generate(String targetType, String targetId, String operation, Drop drop) {
        String method = "entity".equalsIgnoreCase(targetType) ? "modifyEntity" : "modifyBlock";
        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.lootTables(event => {\n");
        sb.append("  event.").append(method).append("('").append(esc(targetId)).append("', table => {\n");
        switch (operation) {
            case "clear" -> sb.append("    table.clear()\n");
            case "removeDrop" -> sb.append("    table.removeItem('")
                    .append(esc(drop != null ? drop.itemId() : "")).append("')\n");
            case "addDrop" -> appendAddDrop(sb, drop);
            default -> sb.append("    // unknown operation '").append(esc(operation)).append("'\n");
        }
        sb.append("  })\n");
        sb.append("})\n");
        return sb.toString();
    }

    private static void appendAddDrop(StringBuilder sb, Drop drop) {
        String item = drop != null ? drop.itemId() : "minecraft:air";
        double chance = drop != null ? drop.chance() : 1.0;
        int min = drop != null ? drop.countMin() : 1;
        int max = drop != null ? drop.countMax() : 1;
        sb.append("    table.addPool(pool => {\n");
        sb.append("      pool.addItem('").append(esc(item)).append("')");
        if (min != 1 || max != 1) {
            sb.append("\n        .count([").append(min).append(", ").append(max).append("])");
        }
        if (chance < 1.0) {
            sb.append("\n        .randomChance(").append(trimNum(chance)).append(')');
        }
        sb.append("\n    })\n");
    }

    private static String trimNum(double d) {
        if (d == Math.floor(d)) {
            return String.valueOf((int) d);
        }
        return String.valueOf(d);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
