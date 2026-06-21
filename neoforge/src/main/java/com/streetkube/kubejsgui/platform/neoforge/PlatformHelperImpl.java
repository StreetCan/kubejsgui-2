package com.streetkube.kubejsgui.platform.neoforge;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Path;

public class PlatformHelperImpl {
    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
