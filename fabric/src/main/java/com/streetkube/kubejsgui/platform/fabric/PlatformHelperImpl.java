package com.streetkube.kubejsgui.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class PlatformHelperImpl {
    public static MinecraftServer getServer() {
        return FabricServerHolder.server;
    }

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}
