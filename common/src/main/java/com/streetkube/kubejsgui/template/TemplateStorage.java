package com.streetkube.kubejsgui.template;

import java.nio.file.Path;

import com.streetkube.kubejsgui.platform.PlatformHelper;

/**
 * Resolves the two template storage roots (universal + per-instance) and enforces
 * path-traversal protection for every file the HTTP server reads or writes.
 */
public final class TemplateStorage {

    public static final String FILE_EXT = ".kubeguitemplate";

    private TemplateStorage() {
    }

    /** Cross-instance template directory, location depends on the OS. */
    public static Path universalRoot() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path base = appData != null && !appData.isBlank()
                    ? Path.of(appData)
                    : Path.of(System.getProperty("user.home"), "AppData", "Roaming");
            return base.resolve("kubejs-gui-templates");
        }
        if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"),
                    "Library", "Application Support", "kubejs-gui-templates");
        }
        return Path.of(System.getProperty("user.home"), ".config", "kubejs-gui-templates");
    }

    /** Per-instance template directory under the game directory. */
    public static Path instanceRoot() {
        return PlatformHelper.getGameDir()
                .resolve("kubejs")
                .resolve("kubejs_gui_templates");
    }

    public static Path rootFor(String location) {
        return "universal".equalsIgnoreCase(location) ? universalRoot() : instanceRoot();
    }

    /**
     * Resolves {@code relativePath} under {@code root}, rejecting traversal attempts.
     *
     * @return the normalized absolute path, or {@code null} if the path is unsafe.
     */
    public static Path resolveSafe(Path root, String relativePath) {
        if (!isValidRelative(relativePath)) {
            return null;
        }
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root.normalize())) {
            return null;
        }
        return resolved;
    }

    /**
     * Validates an absolute path supplied by the client (e.g. from the folder tree),
     * confirming it lives under one of the two known roots.
     *
     * @return the normalized path if safe, otherwise {@code null}.
     */
    public static Path validateAbsolute(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return null;
        }
        Path resolved;
        try {
            resolved = Path.of(absolutePath).normalize();
        } catch (RuntimeException e) {
            return null;
        }
        if (resolved.startsWith(universalRoot().normalize())
                || resolved.startsWith(instanceRoot().normalize())) {
            return resolved;
        }
        return null;
    }

    private static boolean isValidRelative(String p) {
        if (p == null || p.isBlank()) {
            return false;
        }
        if (p.contains("..") || p.startsWith("/") || p.startsWith("\\") || p.contains(":")) {
            return false;
        }
        return p.matches("[a-zA-Z0-9_\\-/. ]+");
    }
}
