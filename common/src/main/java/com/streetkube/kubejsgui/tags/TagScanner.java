package com.streetkube.kubejsgui.tags;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans the server-side tag registries (item/block/fluid/entity) into {@link TagEntry}
 * lists. A heavily modded instance can have thousands of tags, so each type's result is
 * cached after the first scan; {@link #invalidate()} clears the cache for a manual refresh.
 */
public final class TagScanner {

    private static final Map<String, List<TagEntry>> CACHE = new ConcurrentHashMap<>();

    private TagScanner() {
    }

    public static void invalidate() {
        CACHE.clear();
    }

    public static List<TagEntry> scan(MinecraftServer server, String type) {
        String key = normalize(type);
        List<TagEntry> cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        RegistryAccess ra = server.registryAccess();
        List<TagEntry> result = switch (key) {
            case "block" -> scanRegistry(ra.registryOrThrow(Registries.BLOCK));
            case "fluid" -> scanRegistry(ra.registryOrThrow(Registries.FLUID));
            case "entity" -> scanRegistry(ra.registryOrThrow(Registries.ENTITY_TYPE));
            default -> scanRegistry(ra.registryOrThrow(Registries.ITEM));
        };
        CACHE.put(key, result);
        return result;
    }

    public static String normalize(String type) {
        if (type == null) {
            return "item";
        }
        return switch (type.toLowerCase()) {
            case "block", "fluid", "entity" -> type.toLowerCase();
            default -> "item";
        };
    }

    private static <T> List<TagEntry> scanRegistry(Registry<T> registry) {
        List<TagEntry> out = new ArrayList<>();
        registry.getTags().forEach(pair -> {
            String tagId = pair.getFirst().location().toString();
            List<String> members = new ArrayList<>();
            pair.getSecond().forEach(holder ->
                    holder.unwrapKey().ifPresent(k -> members.add(k.location().toString())));
            members.sort(String.CASE_INSENSITIVE_ORDER);
            out.add(new TagEntry(tagId, members));
        });
        out.sort(Comparator.comparing(e -> e.id, String.CASE_INSENSITIVE_ORDER));
        return out;
    }
}
