package com.streetkube.kubejsgui.platform.fabric;

import net.minecraft.server.MinecraftServer;

public final class FabricServerHolder {
    public static volatile MinecraftServer server;

    private FabricServerHolder() {
    }
}
