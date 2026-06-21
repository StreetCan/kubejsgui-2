package com.streetkube.kubejsgui.community;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import com.streetkube.kubejsgui.platform.PlatformHelper;
import com.streetkube.kubejsgui.template.TemplateFile;
import com.streetkube.kubejsgui.template.TemplateParser;

/**
 * Downloads, validates and installs a single community entry. Performs the full ordered
 * security check list before writing anything to disk.
 */
public final class CommunityInstaller {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final int MAX_BYTES = 500_000;

    private CommunityInstaller() {
    }

    public static final class Result {
        public boolean success;
        public boolean updated;
        public String installedTo;
        public String message;
        public String error;

        static Result error(String error) {
            Result r = new Result();
            r.error = error;
            return r;
        }
    }

    public static Result install(String fileUrl, String expectedType, String expectedName) {
        if (fileUrl == null || !fileUrl.toLowerCase().startsWith("https://")) {
            return Result.error("Only https:// URLs are allowed");
        }
        URI fileUri;
        try {
            fileUri = URI.create(fileUrl);
        } catch (RuntimeException e) {
            return Result.error("Invalid file URL");
        }

        // Domain must belong to a configured repo (prevents arbitrary off-repo fetches).
        Set<String> hosts = CommunityCache.activeHosts();
        if (fileUri.getHost() == null || !hosts.contains(fileUri.getHost())) {
            return Result.error("File URL domain does not match any configured repo. Rejected.");
        }

        String body;
        try {
            HttpRequest request = HttpRequest.newBuilder(fileUri)
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Result.error("HTTP " + response.statusCode() + " fetching file");
            }
            body = response.body();
        } catch (Exception e) {
            return Result.error("Failed to fetch: " + e.getMessage());
        }
        if (body.length() > MAX_BYTES) {
            return Result.error("File exceeds 500KB limit");
        }

        TemplateFile parsed = TemplateParser.parse(body);
        String type = parsed.getType();
        if (expectedType != null && !expectedType.equalsIgnoreCase(type)) {
            return Result.error("Type mismatch: index.json says '" + expectedType
                    + "' but file contains type '" + type + "'. Aborted.");
        }

        String name = (expectedName != null && !expectedName.isBlank()) ? expectedName : parsed.getName();
        String safe = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safe.isEmpty()) {
            return Result.error("Invalid entry name");
        }

        Path gameDir = PlatformHelper.getGameDir();
        Path kubejs = gameDir.resolve("kubejs");
        Path outputDir;
        String ext;
        String content;
        switch (type) {
            case "server" -> { outputDir = kubejs.resolve("server_scripts").resolve("community"); ext = ".js"; content = parsed.getScriptTemplate(); }
            case "client" -> { outputDir = kubejs.resolve("client_scripts").resolve("community"); ext = ".js"; content = parsed.getScriptTemplate(); }
            case "startup" -> { outputDir = kubejs.resolve("startup_scripts").resolve("community"); ext = ".js"; content = parsed.getScriptTemplate(); }
            case "template" -> { outputDir = kubejs.resolve("kubejs_gui_templates").resolve("community"); ext = ".kubeguitemplate"; content = body; }
            default -> { return Result.error("Unknown content type '" + type + "'"); }
        }

        try {
            Files.createDirectories(outputDir);
            Path resolved = outputDir.resolve(safe + ext).normalize();
            if (!resolved.startsWith(outputDir.normalize())) {
                return Result.error("Path traversal attempt rejected");
            }
            boolean updated = Files.exists(resolved);
            Files.writeString(resolved, content, StandardCharsets.UTF_8);

            String rel = gameDir.relativize(resolved).toString().replace('\\', '/');
            notifyPlayers(name, rel, type, updated);

            Result r = new Result();
            r.success = true;
            r.updated = updated;
            r.installedTo = rel;
            r.message = (updated ? name + " updated." : name + " installed.")
                    + ("template".equals(type) ? "" : " Run /kubejs reload to apply.");
            return r;
        } catch (Exception e) {
            return Result.error("Write failed: " + e.getMessage());
        }
    }

    private static void notifyPlayers(String name, String rel, String type, boolean updated) {
        MinecraftServer server = PlatformHelper.getServer();
        if (server == null) {
            return;
        }
        String reload = "template".equals(type) ? "" : " Run /kubejs reload to apply.";
        String verb = updated ? "updated" : "installed";
        server.execute(() -> {
            Component message = Component.literal(
                    "[KubeJS GUI] " + name + " " + verb + " to " + rel + "." + reload);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        });
    }
}
