package com.streetkube.kubejsgui.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Server-side singleton tracking the single tagged chest (MVP is singleplayer-scoped).
 * Also holds the volatile inventory snapshot shared with the HTTP server / SSE clients.
 */
public final class TaggedChestState {

    public record ChestLocation(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static volatile ChestLocation tagged;
    private static volatile String inventoryJson = "[]";
    private static volatile int inventoryHash = 0;

    private TaggedChestState() {
    }

    public static void set(ResourceKey<Level> dimension, BlockPos pos) {
        tagged = new ChestLocation(dimension, pos);
        inventoryJson = "[]";
        inventoryHash = 0;
    }

    public static Optional<ChestLocation> get() {
        return Optional.ofNullable(tagged);
    }

    public static String getInventoryJson() {
        return inventoryJson;
    }

    public static void setInventoryJson(String json) {
        inventoryJson = json;
    }

    public static int getInventoryHash() {
        return inventoryHash;
    }

    public static void setInventoryHash(int hash) {
        inventoryHash = hash;
    }
}
