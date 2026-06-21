package com.streetkube.kubejsgui.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Scans the frozen fluid registry for the set of fluid ids in the instance. The null
 * placeholder {@code minecraft:empty} is excluded — it is never valid in a recipe. The
 * registry is frozen after load, so the list is scanned once and cached.
 */
public final class FluidScanner {

    private static volatile List<String> cache;

    private FluidScanner() {
    }

    public static List<String> getFluidIds() {
        List<String> cached = cache;
        if (cached != null) {
            return cached;
        }
        cache = BuiltInRegistries.FLUID.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> !id.equals("minecraft:empty"))
                .sorted()
                .toList();
        return cache;
    }
}
