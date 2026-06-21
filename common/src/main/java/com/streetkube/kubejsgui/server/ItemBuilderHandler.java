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
 * Custom-item builder endpoints (items are registered in startup scripts):
 * <ul>
 *   <li>POST /itembuilder/save?id=    - writes the raw script body to
 *       {@code kubejs/startup_scripts/generated/<id>.js}</li>
 *   <li>POST /itembuilder/texture?id= - writes the raw PNG body to
 *       {@code kubejs/assets/kubejs/textures/item/<id>.png} (auto-resolved by KubeJS)</li>
 * </ul>
 */
public final class ItemBuilderHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]+");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"POST".equals(exchange.getRequestMethod())) {
            error(exchange, 405, "Method not allowed");
            return;
        }
        try {
            if (path.equals("/itembuilder/save")) {
                handleSave(exchange);
            } else if (path.equals("/itembuilder/texture")) {
                handleTexture(exchange);
            } else {
                error(exchange, 404, "not found");
            }
        } catch (IOException e) {
            error(exchange, 500, "I/O error: " + e.getMessage());
        }
    }

    private void handleSave(HttpExchange exchange) throws IOException {
        String id = parseQuery(exchange).get("id");
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            error(exchange, 400, "Item ID must use only lowercase letters, numbers and underscores");
            return;
        }
        String script = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        Path dir = PlatformHelper.getGameDir()
                .resolve("kubejs").resolve("startup_scripts").resolve("generated");
        Files.createDirectories(dir);
        Path out = dir.resolve(id + ".js");
        Files.writeString(out, script, StandardCharsets.UTF_8);

        notifyPlayers(id);

        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("path", "kubejs/startup_scripts/generated/" + id + ".js");
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
    }

    private void handleTexture(HttpExchange exchange) throws IOException {
        String id = parseQuery(exchange).get("id");
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            error(exchange, 400, "Invalid item ID for texture");
            return;
        }
        byte[] bytes = exchange.getRequestBody().readAllBytes();

        Path dir = PlatformHelper.getGameDir()
                .resolve("kubejs").resolve("assets").resolve("kubejs")
                .resolve("textures").resolve("item");
        Files.createDirectories(dir);
        Path out = dir.resolve(id + ".png");
        Files.write(out, bytes);

        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("path", "kubejs/assets/kubejs/textures/item/" + id + ".png");
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
    }

    private void notifyPlayers(String id) {
        MinecraftServer server = PlatformHelper.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            Component message = Component.literal(
                    "[KubeJS GUI] Item '" + id + "' written to startup_scripts. "
                            + "Restart the game to apply (custom items cannot be hot-reloaded).");
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
