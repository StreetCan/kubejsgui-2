package com.streetkube.kubejsgui.template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of a .kubeguitemplate file: metadata key/value pairs, the canvas
 * elements, and the raw KubeJS script template (everything between SCRIPT_START/SCRIPT_END).
 */
public final class TemplateFile {

    private final Map<String, String> metadata = new LinkedHashMap<>();
    private final List<TemplateElement> elements = new ArrayList<>();
    private String scriptTemplate = "";

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<TemplateElement> getElements() {
        return elements;
    }

    public String getScriptTemplate() {
        return scriptTemplate;
    }

    public void setScriptTemplate(String scriptTemplate) {
        this.scriptTemplate = scriptTemplate;
    }

    public String getName() {
        return metadata.getOrDefault("name", "");
    }

    public String getAuthor() {
        return metadata.getOrDefault("author", "");
    }

    /** Content type: server | client | startup | template. Defaults to template (backwards compatible). */
    public String getType() {
        return metadata.getOrDefault("type", "template");
    }
}
