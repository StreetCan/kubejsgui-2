package com.streetkube.kubejsgui.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles the "hold bedrock, right-click a chest" interaction that designates the
 * chest as the script builder's item palette.
 */
public final class ChestTagger {

    private ChestTagger() {
    }

    /**
     * @return true if the chest at {@code pos} was successfully (re)tagged.
     */
    public static boolean tryTagChest(Player player, Level level, BlockPos pos, BlockState state, ItemStack heldItem) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!heldItem.is(Items.BEDROCK)) {
            return false;
        }
        if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST)) {
            return false;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return false;
        }

        untagPrevious(serverLevel.getServer());

        ((TaggedChestAccessor) chest).streetskubegui$setTagged(true);
        chest.setChanged();

        TaggedChestState.set(serverLevel.dimension(), pos.immutable());

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "[KubeJS GUI] Chest at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                            + " tagged as script palette."
            ));
        }

        return true;
    }

    private static void untagPrevious(MinecraftServer server) {
        TaggedChestState.get().ifPresent(loc -> {
            ServerLevel previousLevel = server.getLevel(loc.dimension());
            if (previousLevel == null) {
                return;
            }
            BlockEntity previous = previousLevel.getBlockEntity(loc.pos());
            if (previous instanceof TaggedChestAccessor accessor) {
                accessor.streetskubegui$setTagged(false);
                previous.setChanged();
            }
        });
    }
}
