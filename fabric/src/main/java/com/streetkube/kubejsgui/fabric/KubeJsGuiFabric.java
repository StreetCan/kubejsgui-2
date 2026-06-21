package com.streetkube.kubejsgui.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.streetkube.kubejsgui.KubeJsGui;
import com.streetkube.kubejsgui.block.InventoryWatcher;
import com.streetkube.kubejsgui.platform.fabric.FabricServerHolder;
import com.streetkube.kubejsgui.server.ScriptBuilderServer;

public final class KubeJsGuiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> FabricServerHolder.server = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ScriptBuilderServer.stop();
            FabricServerHolder.server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(InventoryWatcher::tick);

        EventHandlerFabric.register();

        KubeJsGui.init();
    }
}
