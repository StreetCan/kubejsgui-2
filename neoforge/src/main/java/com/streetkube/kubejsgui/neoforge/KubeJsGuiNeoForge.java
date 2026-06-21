package com.streetkube.kubejsgui.neoforge;

import net.neoforged.fml.common.Mod;

import com.streetkube.kubejsgui.KubeJsGui;

@Mod(KubeJsGui.MOD_ID)
public final class KubeJsGuiNeoForge {
    public KubeJsGuiNeoForge() {
        KubeJsGui.init();
    }
}
