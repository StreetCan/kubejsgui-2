package com.streetkube.kubejsgui.template;

/**
 * Parses .kubeguitemplate text into a {@link TemplateFile}.
 *
 * <p>Rules (per spec):
 * <ul>
 *   <li>Lines before any element/section are metadata {@code key;value} pairs.</li>
 *   <li>Canvas element lines start with one of {@code slot}, {@code label}, {@code arrow},
 *       {@code input}.</li>
 *   <li>{@code SCRIPT_START}/{@code SCRIPT_END} bracket the raw script; everything between
 *       (including blank and {@code #} lines) is preserved verbatim.</li>
 *   <li>Outside the script block, blank lines and {@code #} comments are ignored.</li>
 * </ul>
 */
public final class TemplateParser {

    private static final String SCRIPT_START = "SCRIPT_START";
    private static final String SCRIPT_END = "SCRIPT_END";

    private TemplateParser() {
    }

    public static TemplateFile parse(String text) {
        TemplateFile file = new TemplateFile();
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);

        boolean inScript = false;
        StringBuilder script = new StringBuilder();

        for (String line : lines) {
            if (!inScript && line.trim().equals(SCRIPT_START)) {
                inScript = true;
                continue;
            }
            if (inScript) {
                if (line.trim().equals(SCRIPT_END)) {
                    inScript = false;
                    continue;
                }
                script.append(line).append('\n');
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String firstToken = firstToken(trimmed);
            if (isElementToken(firstToken)) {
                file.getElements().add(TemplateElement.parse(trimmed));
            } else {
                int sep = trimmed.indexOf(';');
                if (sep > 0) {
                    String key = trimmed.substring(0, sep).trim();
                    String value = trimmed.substring(sep + 1).trim();
                    file.getMetadata().put(key, value);
                }
            }
        }

        // Strip the single trailing newline we always add per script line.
        if (script.length() > 0 && script.charAt(script.length() - 1) == '\n') {
            script.setLength(script.length() - 1);
        }
        file.setScriptTemplate(script.toString());
        return file;
    }

    private static String firstToken(String line) {
        int sep = line.indexOf(';');
        return sep < 0 ? line : line.substring(0, sep).trim();
    }

    private static boolean isElementToken(String token) {
        return token.equals("slot") || token.equals("label")
                || token.equals("arrow") || token.equals("input")
                || token.equals("fluidslot") || token.equals("list")
                || token.equals("fluidid") || token.equals("tagpick");
    }
}
