package com.streetkube.kubejsgui.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class PlatformHelper {
    @ExpectPlatform
    public static MinecraftServer getServer() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError();
    }
}
