package com.streetkube.kubejsgui.server;

import java.util.Map;

/**
 * Body of POST /script: a recipe type plus the slot contents the user dropped into
 * the builder grid, ready to be turned into a KubeJS recipe script.
 */
public record ScriptRequest(
        String type,
        String scriptId,
        Map<String, String> slots,
        OutputSpec output,
        Map<String, Object> extraData,
        // Template-based generation: when templateContent is present the request is rendered
        // through TemplateScriptGenerator with the values map instead of the hardcoded
        // recipe generators.
        String templateContent,
        Map<String, String> values
) {
    public record OutputSpec(String id, int count) {
    }
}
