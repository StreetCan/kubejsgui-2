package com.streetkube.kubejsgui.modify;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.streetkube.kubejsgui.platform.PlatformHelper;
import com.streetkube.kubejsgui.server.ScriptBuilderServer;

/**
 * HTTP endpoints for the Modify tab.
 * <ul>
 *   <li>POST /modify/recipe   - recipe remove/replace script (server)</li>
 *   <li>POST /modify/item     - item modification script(s) (client and/or startup)</li>
 *   <li>POST /modify/loot     - loot table modification script (server)</li>
 *   <li>GET  /loottable?type=&amp;id= - best-effort current drops for a loot target</li>
 * </ul>
 */
public final class ModifyHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final Pattern SCRIPT_ID = Pattern.compile("[a-z0-9_]+");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if (path.equals("/modify/recipe") && method.equals("POST")) {
                handleRecipe(exchange);
            } else if (path.equals("/modify/item") && method.equals("POST")) {
                handleItem(exchange);
            } else if (path.equals("/modify/loot") && method.equals("POST")) {
                handleLoot(exchange);
            } else if (path.equals("/loottable") && method.equals("GET")) {
                handleLootScan(exchange);
            } else {
                error(exchange, 404, "not found");
            }
        } catch (IOException e) {
            error(exchange, 500, "I/O error: " + e.getMessage());
        }
    }

    /* ---- recipe ---- */

    private void handleRecipe(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String scriptId = optString(body, "scriptId");
        if (!validId(scriptId)) {
            error(exchange, 400, "Script ID must use only lowercase letters, numbers and underscores");
            return;
        }
        String operation = optString(body, "operation");
        if (operation == null || operation.isBlank()) {
            error(exchange, 400, "Missing operation");
            return;
        }

        String script;
        if (operation.startsWith("replace")) {
            Map<String, String> scope = toStringMap(body.has("scopeFilters") ? body.getAsJsonObject("scopeFilters") : null);
            String find = optString(body, "find");
            String replace = optString(body, "replace");
            if (find == null || find.isBlank() || replace == null || replace.isBlank()) {
                error(exchange, 400, "Replace operations require both a find and a replace item");
                return;
            }
            script = operation.equals("replaceOutput")
                    ? RecipeModifier.generateReplaceOutput(scope, find, replace)
                    : RecipeModifier.generateReplaceInput(scope, find, replace);
        } else {
            Map<String, String> filters = toStringMap(body.has("filters") ? body.getAsJsonObject("filters") : null);
            script = RecipeModifier.generateRemove(filters);
        }

        try {
            Path file = writeScript("server_scripts", scriptId + ".js", script);
            notify(scriptId + ".js", "server");
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.add("filesGenerated", oneFile(rel(file), "server", "kubejs reload"));
            sendJson(exchange, json);
        } catch (IOException e) {
            error(exchange, 500, "Write failed: " + e.getMessage());
        }
    }

    /* ---- item ---- */

    private void handleItem(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String scriptId = optString(body, "scriptId");
        if (!validId(scriptId)) {
            error(exchange, 400, "Script ID must use only lowercase letters, numbers and underscores");
            return;
        }
        String itemId = optString(body, "itemId");
        if (itemId == null || itemId.isBlank()) {
            error(exchange, 400, "Missing target item");
            return;
        }
        JsonObject ops = body.has("operations") && body.get("operations").isJsonObject()
                ? body.getAsJsonObject("operations") : new JsonObject();

        boolean hideFromJei = ops.has("hideFromJei") && ops.get("hideFromJei").getAsBoolean();
        boolean banItem = ops.has("banItem") && ops.get("banItem").getAsBoolean();
        Integer maxDamage = null;
        Integer maxStackSize = null;
        String displayName = null;
        if (ops.has("properties") && ops.get("properties").isJsonObject()) {
            JsonObject props = ops.getAsJsonObject("properties");
            maxDamage = optInt(props, "maxDamage");
            maxStackSize = optInt(props, "maxStackSize");
            displayName = optString(props, "displayName");
            if (displayName != null && displayName.isBlank()) {
                displayName = null;
            }
        }

        String client = ItemModifier.generateClient(itemId, hideFromJei);
        String startup = ItemModifier.generateStartup(itemId, banItem, maxDamage, maxStackSize, displayName);
        if (client == null && startup == null) {
            error(exchange, 400, "No operations selected");
            return;
        }

        try {
            JsonArray files = new JsonArray();
            if (client != null) {
                Path f = writeScript("client_scripts", scriptId + ".js", client);
                files.add(fileEntry(rel(f), "client", "kubejs reload"));
                notify(scriptId + ".js", "client");
            }
            if (startup != null) {
                Path f = writeScript("startup_scripts", scriptId + "_startup.js", startup);
                files.add(fileEntry(rel(f), "startup", "full restart"));
                notify(scriptId + "_startup.js", "startup");
            }
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.add("filesGenerated", files);
            sendJson(exchange, json);
        } catch (IOException e) {
            error(exchange, 500, "Write failed: " + e.getMessage());
        }
    }

    /* ---- loot ---- */

    private void handleLoot(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String scriptId = optString(body, "scriptId");
        if (!validId(scriptId)) {
            error(exchange, 400, "Script ID must use only lowercase letters, numbers and underscores");
            return;
        }
        String targetType = optString(body, "targetType");
        String targetId = optString(body, "targetId");
        String operation = optString(body, "operation");
        if (targetId == null || targetId.isBlank()) {
            error(exchange, 400, "Missing target id");
            return;
        }
        if ("chest".equalsIgnoreCase(targetType)) {
            error(exchange, 400, "Chest loot tables are not supported yet");
            return;
        }

        LootTableModifier.Drop drop = null;
        if (body.has("drop") && body.get("drop").isJsonObject()) {
            JsonObject d = body.getAsJsonObject("drop");
            Integer min = optInt(d, "countMin");
            Integer max = optInt(d, "countMax");
            Double chance = d.has("chance") && !d.get("chance").isJsonNull() ? d.get("chance").getAsDouble() : null;
            drop = new LootTableModifier.Drop(
                    optString(d, "itemId"),
                    chance != null ? chance : 1.0,
                    min != null ? min : 1,
                    max != null ? max : 1);
        }
        if ("addDrop".equals(operation) && (drop == null || drop.itemId() == null || drop.itemId().isBlank())) {
            error(exchange, 400, "Add Drop requires an item");
            return;
        }
        if ("removeDrop".equals(operation) && (drop == null || drop.itemId() == null || drop.itemId().isBlank())) {
            error(exchange, 400, "Remove Drop requires an item");
            return;
        }

        String script = LootTableModifier.generate(targetType, targetId, operation, drop);
        try {
            Path file = writeScript("server_scripts", scriptId + ".js", script);
            notify(scriptId + ".js", "server");
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.add("filesGenerated", oneFile(rel(file), "server", "kubejs reload"));
            sendJson(exchange, json);
        } catch (IOException e) {
            error(exchange, 500, "Write failed: " + e.getMessage());
        }
    }

    private void handleLootScan(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        LootTableScanner.Result result = LootTableScanner.scan(
                query.getOrDefault("type", "block"), query.get("id"));
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(result));
    }

    /* ---- helpers ---- */

    private static Path writeScript(String scriptsFolder, String fileName, String content) throws IOException {
        Path dir = PlatformHelper.getGameDir().resolve("kubejs").resolve(scriptsFolder).resolve("generated");
        Files.createDirectories(dir);
        Path out = dir.resolve(fileName);
        Files.writeString(out, content, StandardCharsets.UTF_8);
        return out;
    }

    private static String rel(Path file) {
        return PlatformHelper.getGameDir().relativize(file).toString().replace('\\', '/');
    }

    private static JsonArray oneFile(String path, String type, String applyWith) {
        JsonArray arr = new JsonArray();
        arr.add(fileEntry(path, type, applyWith));
        return arr;
    }

    private static JsonObject fileEntry(String path, String type, String applyWith) {
        JsonObject o = new JsonObject();
        o.addProperty("path", path);
        o.addProperty("type", type);
        o.addProperty("applyWith", applyWith);
        return o;
    }

    private void notify(String fileName, String type) {
        MinecraftServer server = PlatformHelper.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            String msg;
            if ("startup".equals(type)) {
                msg = "[KubeJS GUI] " + fileName + " written to startup_scripts/generated/. "
                        + "⚠ Requires full Minecraft restart — /kubejs reload will NOT apply this.";
            } else {
                msg = "[KubeJS GUI] " + fileName + " written to " + type + "_scripts/generated/. "
                        + "Run /kubejs reload to apply.";
            }
            Component message = Component.literal(msg);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        });
    }

    private static boolean validId(String id) {
        return id != null && !id.isBlank() && SCRIPT_ID.matcher(id).matches();
    }

    private JsonObject readJson(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            return obj != null ? obj : new JsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    private static Map<String, String> toStringMap(JsonObject obj) {
        Map<String, String> out = new LinkedHashMap<>();
        if (obj != null) {
            for (String key : obj.keySet()) {
                if (!obj.get(key).isJsonNull()) {
                    out.put(key, obj.get(key).getAsString());
                }
            }
        }
        return out;
    }

    private static String optString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static Integer optInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void sendJson(HttpExchange exchange, JsonObject json) throws IOException {
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
