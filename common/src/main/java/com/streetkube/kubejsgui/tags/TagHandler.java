package com.streetkube.kubejsgui.tags;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.streetkube.kubejsgui.platform.PlatformHelper;
import com.streetkube.kubejsgui.server.ScriptBuilderServer;

/**
 * Tag endpoints:
 * <ul>
 *   <li>GET  /tags?type=item|block|fluid|entity - all tags of that type + members</li>
 *   <li>POST /tags/refresh - clears the scan cache</li>
 *   <li>POST /tags/generate - writes a tag .kubeguitemplate (+ optional .js)</li>
 * </ul>
 */
public final class TagHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final Pattern SCRIPT_ID = Pattern.compile("[a-z0-9_]+");
    private static final Pattern TAG_ID = Pattern.compile("[a-z0-9_]+:[a-z0-9_/]+");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if (path.equals("/tags") && method.equals("GET")) {
                handleList(exchange);
            } else if (path.equals("/tags/refresh") && method.equals("POST")) {
                TagScanner.invalidate();
                ok(exchange);
            } else if (path.equals("/tags/generate") && method.equals("POST")) {
                handleGenerate(exchange);
            } else {
                error(exchange, 404, "not found");
            }
        } catch (IOException e) {
            error(exchange, 500, "I/O error: " + e.getMessage());
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        String type = TagScanner.normalize(parseQuery(exchange).get("type"));
        MinecraftServer server = PlatformHelper.getServer();
        List<TagEntry> tags = server != null ? TagScanner.scan(server, type) : List.of();
        JsonObject root = new JsonObject();
        root.addProperty("type", type);
        root.add("tags", GSON.toJsonTree(tags));
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(root));
    }

    private void handleGenerate(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String scriptId = optString(body, "scriptId");
        if (scriptId == null || !SCRIPT_ID.matcher(scriptId).matches()) {
            error(exchange, 400, "Script ID must use only lowercase letters, numbers and underscores");
            return;
        }
        String tagType = TagScanner.normalize(optString(body, "tagType"));
        String operation = "remove".equals(optString(body, "operation")) ? "remove" : "add";
        String tagId = optString(body, "tagId");
        if (tagId == null || !TAG_ID.matcher(tagId).matches()) {
            error(exchange, 400, "Tag ID must look like namespace:path (lowercase, digits, _ and /)");
            return;
        }
        List<String> members = stringList(body.get("members"));
        if (members.isEmpty()) {
            error(exchange, 400, "Add at least one member");
            return;
        }
        String author = optString(body, "author");
        if (author == null || author.isBlank()) {
            author = "unknown";
        }
        boolean alsoScript = body.has("alsoGenerateScript") && body.get("alsoGenerateScript").getAsBoolean();
        String description = optString(body, "description");

        try {
            TagTemplateWriter.Result result = TagTemplateWriter.write(
                    scriptId, tagType, operation, tagId, members, author, description, alsoScript);
            notifyPlayers(scriptId);

            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("templatePath", result.templatePath);
            if (result.scriptPath != null) {
                json.addProperty("scriptPath", result.scriptPath);
            } else {
                json.add("scriptPath", null);
            }
            json.addProperty("requiresRestart", true);
            ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
        } catch (IOException e) {
            error(exchange, 500, "Write failed: " + e.getMessage());
        }
    }

    private void notifyPlayers(String scriptId) {
        MinecraftServer server = PlatformHelper.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            Component message = Component.literal("[KubeJS GUI] Tag template saved: " + scriptId
                    + ". ⚠ Startup scripts require a full Minecraft restart, not /kubejs reload.");
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        });
    }

    /* ---- helpers ---- */

    private static List<String> stringList(JsonElement el) {
        List<String> out = new ArrayList<>();
        if (el != null && el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonNull()) {
                    String s = e.getAsString().trim();
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }

    private JsonObject readJson(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            return obj != null ? obj : new JsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    private static String optString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private void ok(HttpExchange exchange) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
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
