package com.streetkube.kubejsgui.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.streetkube.kubejsgui.KubeJsGui;
import com.streetkube.kubejsgui.block.ChestTagger;
import com.streetkube.kubejsgui.block.InventoryWatcher;
import com.streetkube.kubejsgui.server.ScriptBuilderServer;

@EventBusSubscriber(modid = KubeJsGui.MOD_ID)
public final class EventHandlerNeoForge {

    private EventHandlerNeoForge() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        if (ChestTagger.tryTagChest(player, level, pos, state, event.getItemStack())) {
            ScriptBuilderServer.ensureStarted();
            if (player instanceof ServerPlayer serverPlayer) {
                ScriptBuilderServer.sendReadyMessage(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ScriptBuilderServer.stop();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        InventoryWatcher.tick(event.getServer());
    }
}
