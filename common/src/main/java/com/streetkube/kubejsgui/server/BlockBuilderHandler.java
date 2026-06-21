package com.streetkube.kubejsgui.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.streetkube.kubejsgui.platform.PlatformHelper;

/**
 * Block builder endpoint:
 * <ul>
 *   <li>POST /blockbuilder/save?id=&amp;dest=startup|server - writes the raw script body to
 *       {@code kubejs/<startup|server>_scripts/generated/<id>.js}</li>
 * </ul>
 *
 * <p>Block property modification ({@code BlockEvents.modification}) belongs in startup scripts;
 * block interaction events ({@code BlockEvents.broken/placed/leftClicked/rightClicked}) belong in
 * server scripts. The frontend chooses {@code dest} accordingly.
 */
public final class BlockBuilderHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]+");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod()) || !exchange.getRequestURI().getPath().equals("/blockbuilder/save")) {
            error(exchange, 404, "not found");
            return;
        }
        try {
            Map<String, String> query = parseQuery(exchange);
            String id = query.get("id");
            if (id == null || !ID_PATTERN.matcher(id).matches()) {
                error(exchange, 400, "Script ID must use only lowercase letters, numbers and underscores");
                return;
            }
            String dest = query.getOrDefault("dest", "server");
            String folder = "startup".equalsIgnoreCase(dest) ? "startup_scripts" : "server_scripts";

            String script = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            Path dir = PlatformHelper.getGameDir().resolve("kubejs").resolve(folder).resolve("generated");
            Files.createDirectories(dir);
            Path out = dir.resolve(id + ".js");
            Files.writeString(out, script, StandardCharsets.UTF_8);

            notifyPlayers(id, folder);

            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("path", "kubejs/" + folder + "/generated/" + id + ".js");
            ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
        } catch (IOException e) {
            error(exchange, 500, "Failed to write script: " + e.getMessage());
        }
    }

    private void notifyPlayers(String id, String folder) {
        MinecraftServer server = PlatformHelper.getServer();
        if (server == null) {
            return;
        }
        String how = "startup_scripts".equals(folder)
                ? "Restart the game to apply."
                : "Run /kubejs reload to apply.";
        server.execute(() -> {
            Component message = Component.literal("[KubeJS GUI] Block script '" + id + ".js' written to "
                    + folder + ". " + how);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        });
    }

    private void error(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", false);
        json.addProperty("error", message);
        ScriptBuilderServer.sendText(exchange, status, "application/json", GSON.toJson(json));
    }

    private static Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                result.put(decode(pair), "");
            } else {
                result.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return result;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
