package com.streetkube.kubejsgui.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans the frozen game registries for items (with display/durability/stack metadata),
 * blocks and entity types. Results are immutable after registry freeze, so each list is
 * scanned once and cached for the lifetime of the JVM.
 *
 * <p>These are distinct from the tagged-chest inventory served by {@code GET /items}: the
 * registry endpoints return every id known to the game, used to power search/autocomplete
 * fields in the Modify tab.
 */
public final class RegistryScanner {

    /** Lightweight per-item metadata used as read-only hints in the item modifier UI. */
    public record ItemInfo(String id, String displayName, int maxDamage, int maxStackSize) {
    }

    private static volatile List<ItemInfo> itemCache;
    private static volatile List<String> blockCache;
    private static volatile List<String> entityCache;

    private RegistryScanner() {
    }

    public static List<ItemInfo> getItems() {
        List<ItemInfo> cached = itemCache;
        if (cached != null) {
            return cached;
        }
        List<ItemInfo> out = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet().stream().sorted().toList()) {
            Item item = BuiltInRegistries.ITEM.get(id);
            String name = id.toString();
            int maxDamage = 0;
            int maxStack = 64;
            try {
                ItemStack stack = new ItemStack(item);
                name = stack.getHoverName().getString();
                maxDamage = stack.getMaxDamage();
                maxStack = stack.getMaxStackSize();
            } catch (RuntimeException ignored) {
                // Some modded items throw when stack-constructed outside a world context;
                // fall back to the id and vanilla defaults rather than dropping the entry.
            }
            out.add(new ItemInfo(id.toString(), name, maxDamage, maxStack));
        }
        itemCache = List.copyOf(out);
        return itemCache;
    }

    public static List<String> getBlocks() {
        List<String> cached = blockCache;
        if (cached != null) {
            return cached;
        }
        blockCache = BuiltInRegistries.BLOCK.keySet().stream()
                .map(ResourceLocation::toString).sorted().toList();
        return blockCache;
    }

    public static List<String> getEntities() {
        List<String> cached = entityCache;
        if (cached != null) {
            return cached;
        }
        entityCache = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> !id.equals("minecraft:player"))
                .sorted().toList();
        return entityCache;
    }
}
