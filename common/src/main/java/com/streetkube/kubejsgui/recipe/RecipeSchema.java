package com.streetkube.kubejsgui.recipe;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Slot layout schema returned by GET /recipes/schema/:type, describing how the web UI
 * should render the recipe builder for a given recipe type.
 */
public record RecipeSchema(
        String typeId,
        String displayName,
        String shape,
        Integer gridWidth,
        Integer gridHeight,
        boolean hasOutput,
        List<ExtraField> extraFields
) {
    /**
     * @param options non-null only for "enum"-typed fields; the allowed values.
     */
    public record ExtraField(
            String key,
            String type,
            @SerializedName("default") Object defaultValue,
            List<String> options
    ) {
        public ExtraField(String key, String type, Object defaultValue) {
            this(key, type, defaultValue, null);
        }
    }
}
