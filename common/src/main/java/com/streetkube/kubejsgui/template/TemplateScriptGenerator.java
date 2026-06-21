package com.streetkube.kubejsgui.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code {{variable}}} placeholders in a template's raw script with the values
 * the user supplied in the builder.
 *
 * <p>Core behaviour is a simple {@link String#replace} loop, as described in the spec. In
 * addition, optional sections of the form {@code {{#key}}...{{/key}}} are supported: the
 * inner content is kept when {@code key} resolves to a "truthy" value (present, non-empty,
 * and not {@code "false"}/{@code "none"}/{@code "0"}), otherwise the whole section is removed.
 * The frontend supplies derived flag keys (e.g. {@code heat_heated}) so enum-driven options
 * like {@code {{#heat_heated}}.heated(){{/heat_heated}}} work.
 */
public final class TemplateScriptGenerator {

    private static final Pattern SECTION =
            Pattern.compile("\\{\\{#([a-z0-9_]+)\\}\\}(.*?)\\{\\{/\\1\\}\\}", Pattern.DOTALL);

    private TemplateScriptGenerator() {
    }

    public static String generate(TemplateFile template, Map<String, String> values) {
        // This path is only reached when the user generates from a filled-in template, so we
        // always substitute. (server/client/startup files have no {{variable}} placeholders, so
        // substitution is a harmless no-op for them.)
        return generate(template.getScriptTemplate(), values);
    }

    public static String generate(String scriptTemplate, Map<String, String> values) {
        String script = applySections(scriptTemplate, values);

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String replacement = entry.getValue() == null ? "" : entry.getValue();
            script = script.replace("{{" + entry.getKey() + "}}", replacement);
        }

        return script;
    }

    private static String applySections(String script, Map<String, String> values) {
        String current = script;
        // Loop so nested sections resolve fully.
        while (true) {
            Matcher matcher = SECTION.matcher(current);
            StringBuilder out = new StringBuilder();
            boolean matched = false;

            int last = 0;
            while (matcher.find()) {
                matched = true;
                out.append(current, last, matcher.start());
                if (isTruthy(values.get(matcher.group(1)))) {
                    out.append(matcher.group(2));
                }
                last = matcher.end();
            }

            if (!matched) {
                return current;
            }
            out.append(current.substring(last));
            current = out.toString();
        }
    }

    private static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        return !v.isEmpty() && !v.equalsIgnoreCase("false")
                && !v.equalsIgnoreCase("none") && !v.equals("0");
    }
}
