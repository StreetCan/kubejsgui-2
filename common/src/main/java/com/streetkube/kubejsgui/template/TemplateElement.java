package com.streetkube.kubejsgui.template;

import java.util.ArrayList;
import java.util.List;

/**
 * One element placed on a template canvas. Mirrors the line formats in the
 * .kubeguitemplate file spec. Fields not relevant to a given {@link #kind}/{@link #inputType}
 * are left null and are skipped during serialization.
 */
public final class TemplateElement {

    /** slot | label | arrow | input | fluidslot */
    public String kind;
    /** For input elements: text | integer | slider | float | enum. Null otherwise. */
    public String inputType;
    /** Variable name. Null for label/arrow. */
    public String id;
    public int x;
    public int y;
    /** Arrow end point. */
    public Integer x2;
    public Integer y2;
    /** Slot label, input label, or label text. */
    public String label;
    /** Label font size: small | medium | large. */
    public String size;
    /** Arrow direction: right | left | up | down. */
    public String direction;
    /** Text input placeholder. */
    public String placeholder;
    /** Default value for numeric/slider/enum inputs. */
    public String defaultValue;
    public String min;
    public String max;
    public String step;
    /** Enum options. */
    public List<String> options;
    /** Fluid slot default amount in millibuckets. */
    public String amount;
    /** List element formatting: wraps each member as prefix + id + suffix, joined by delimiter. */
    public String prefix;
    public String suffix;
    public String delimiter;
    /** Tag picker registry type: item | block | fluid | entity. */
    public String tagType;

    public static TemplateElement parse(String line) {
        String[] raw = line.split(";", -1);
        String[] t = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            t[i] = raw[i].trim();
        }

        TemplateElement el = new TemplateElement();
        el.kind = t[0];

        switch (t[0]) {
            case "slot" -> {
                el.id = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.label = get(t, 4);
                el.defaultValue = get(t, 5);
            }
            case "label" -> {
                el.label = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.size = orDefault(get(t, 4), "medium");
            }
            case "arrow" -> {
                el.direction = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.x2 = intBox(get(t, 4));
                el.y2 = intBox(get(t, 5));
            }
            case "fluidslot" -> {
                el.id = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.label = get(t, 4);
                el.amount = orDefault(get(t, 5), "1000");
                el.min = get(t, 6);
                el.max = get(t, 7);
            }
            case "list" -> {
                el.id = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.label = get(t, 4);
                el.prefix = t.length > 5 ? t[5] : "'";
                el.suffix = t.length > 6 ? t[6] : "'";
                el.delimiter = t.length > 7 ? t[7] : ",";
            }
            case "fluidid" -> {
                el.id = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.label = get(t, 4);
            }
            case "tagpick" -> {
                el.id = get(t, 1);
                el.x = intOf(get(t, 2));
                el.y = intOf(get(t, 3));
                el.label = get(t, 4);
                el.tagType = orDefault(get(t, 5), "item");
            }
            case "input" -> {
                el.inputType = get(t, 1);
                el.id = get(t, 2);
                el.x = intOf(get(t, 3));
                el.y = intOf(get(t, 4));
                el.label = get(t, 5);
                switch (el.inputType) {
                    case "text" -> el.placeholder = get(t, 6);
                    case "integer", "float" -> {
                        el.defaultValue = get(t, 6);
                        el.min = get(t, 7);
                        el.max = get(t, 8);
                    }
                    case "slider" -> {
                        el.defaultValue = get(t, 6);
                        el.min = get(t, 7);
                        el.max = get(t, 8);
                        el.step = get(t, 9);
                    }
                    case "enum" -> {
                        el.defaultValue = get(t, 6);
                        el.options = parseOptions(get(t, 7));
                    }
                    default -> { }
                }
            }
            default -> { }
        }

        return el;
    }

    public String toLine() {
        StringBuilder sb = new StringBuilder();
        switch (kind) {
            case "slot" -> {
                sb.append("slot;").append(nv(id)).append(';')
                        .append(x).append(';').append(y).append(';').append(nv(label));
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    sb.append(';').append(defaultValue);
                }
            }
            case "label" -> sb.append("label;").append(nv(label)).append(';')
                    .append(x).append(';').append(y).append(';').append(orDefault(size, "medium"));
            case "arrow" -> sb.append("arrow;").append(nv(direction)).append(';')
                    .append(x).append(';').append(y).append(';')
                    .append(x2 != null ? x2 : x).append(';').append(y2 != null ? y2 : y);
            case "fluidslot" -> {
                sb.append("fluidslot;").append(nv(id)).append(';').append(x).append(';')
                        .append(y).append(';').append(nv(label)).append(';').append(orDefault(amount, "1000"));
                boolean hasMin = min != null && !min.isEmpty();
                boolean hasMax = max != null && !max.isEmpty();
                if (hasMin || hasMax) {
                    sb.append(';').append(nv(min)).append(';').append(nv(max));
                }
            }
            case "list" -> sb.append("list;").append(nv(id)).append(';').append(x).append(';')
                    .append(y).append(';').append(nv(label)).append(';')
                    .append(prefix != null ? prefix : "'").append(';')
                    .append(suffix != null ? suffix : "'").append(';')
                    .append(delimiter != null ? delimiter : ",");
            case "fluidid" -> sb.append("fluidid;").append(nv(id)).append(';')
                    .append(x).append(';').append(y).append(';').append(nv(label));
            case "tagpick" -> sb.append("tagpick;").append(nv(id)).append(';')
                    .append(x).append(';').append(y).append(';').append(nv(label)).append(';')
                    .append(orDefault(tagType, "item"));
            case "input" -> {
                sb.append("input;").append(nv(inputType)).append(';').append(nv(id)).append(';')
                        .append(x).append(';').append(y).append(';').append(nv(label));
                switch (inputType == null ? "" : inputType) {
                    case "text" -> sb.append(';').append(nv(placeholder));
                    case "integer", "float" -> sb.append(';').append(nv(defaultValue))
                            .append(';').append(nv(min)).append(';').append(nv(max));
                    case "slider" -> sb.append(';').append(nv(defaultValue))
                            .append(';').append(nv(min)).append(';').append(nv(max))
                            .append(';').append(nv(step));
                    case "enum" -> sb.append(';').append(nv(defaultValue))
                            .append(';').append(joinOptions(options));
                    default -> { }
                }
            }
            default -> { }
        }
        return sb.toString();
    }

    private static List<String> parseOptions(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String joinOptions(List<String> opts) {
        return opts == null ? "" : String.join(",", opts);
    }

    private static String get(String[] arr, int i) {
        return i < arr.length ? arr[i] : "";
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static String nv(String value) {
        return value == null ? "" : value;
    }

    private static int intOf(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Integer intBox(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
