package com.streetkube.kubejsgui.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Converts a chest's container contents into the JSON array format consumed by the
 * web UI's item palette (see GET /items and the SSE inventory_update payload).
 */
public final class InventorySerializer {

    private InventorySerializer() {
    }

    public static JsonArray serialize(Container container) {
        JsonArray array = new JsonArray();

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

            JsonObject entry = new JsonObject();
            entry.addProperty("slot", slot);
            entry.addProperty("id", id.toString());
            entry.addProperty("count", stack.getCount());
            entry.addProperty("displayName", stack.getHoverName().getString());
            entry.add("nbt", new JsonObject());
            array.add(entry);
        }

        return array;
    }
}
