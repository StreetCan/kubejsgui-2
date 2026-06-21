package com.streetkube.kubejsgui.block;

import com.google.gson.JsonArray;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.streetkube.kubejsgui.server.EventsHandler;

/**
 * Called once per server tick (from each platform's tick event) to detect changes to
 * the tagged chest's inventory and push SSE updates when it changes.
 */
public final class InventoryWatcher {

    private InventoryWatcher() {
    }

    public static void tick(MinecraftServer server) {
        TaggedChestState.get().ifPresent(loc -> {
            ServerLevel level = server.getLevel(loc.dimension());
            if (level == null || !level.isLoaded(loc.pos())) {
                return;
            }

            BlockEntity blockEntity = level.getBlockEntity(loc.pos());
            if (!(blockEntity instanceof Container container)) {
                return;
            }

            JsonArray json = InventorySerializer.serialize(container);
            String jsonString = json.toString();
            int hash = jsonString.hashCode();

            if (hash != TaggedChestState.getInventoryHash()) {
                TaggedChestState.setInventoryHash(hash);
                TaggedChestState.setInventoryJson(jsonString);
                EventsHandler.broadcastInventoryUpdate(jsonString);
            }
        });
    }
}
