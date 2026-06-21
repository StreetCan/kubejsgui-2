package com.streetkube.kubejsgui.fabric;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;

import com.streetkube.kubejsgui.block.ChestTagger;
import com.streetkube.kubejsgui.server.ScriptBuilderServer;

public final class EventHandlerFabric {

    private EventHandlerFabric() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (ChestTagger.tryTagChest(player, level, pos, state, player.getItemInHand(hand))) {
                ScriptBuilderServer.ensureStarted();
                if (player instanceof ServerPlayer serverPlayer) {
                    ScriptBuilderServer.sendReadyMessage(serverPlayer);
                }
            }

            return InteractionResult.PASS;
        });
    }
}
