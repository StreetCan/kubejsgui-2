package com.streetkube.kubejsgui.tags;

import java.util.List;

/**
 * Builds the {@code StartupEvents.tags(...)} script body for a tag add/remove operation.
 * Members that are themselves tags carry their own {@code #} prefix and are emitted as-is.
 */
public final class TagScriptGenerator {

    private TagScriptGenerator() {
    }

    public static String generate(String tagType, String operation, String tagId, List<String> members) {
        String op = "remove".equals(operation) ? "remove" : "add";
        StringBuilder sb = new StringBuilder();
        sb.append("StartupEvents.tags('").append(tagType).append("', event => {\n");
        sb.append("  event.").append(op).append("('").append(tagId).append("', [\n");
        for (int i = 0; i < members.size(); i++) {
            sb.append("    '").append(esc(members.get(i))).append('\'');
            sb.append(i < members.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ])\n})\n");
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
