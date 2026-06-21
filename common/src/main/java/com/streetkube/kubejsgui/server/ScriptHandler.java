package com.streetkube.kubejsgui.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.streetkube.kubejsgui.platform.PlatformHelper;
import com.streetkube.kubejsgui.script.ScriptGenerator;
import com.streetkube.kubejsgui.template.TemplateFile;
import com.streetkube.kubejsgui.template.TemplateParser;
import com.streetkube.kubejsgui.template.TemplateScriptGenerator;

/**
 * POST /script: generates a KubeJS recipe script from the builder state and writes it
 * to kubejs/server_scripts/generated/&lt;scriptId&gt;.js.
 */
public final class ScriptHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final java.util.regex.Pattern SCRIPT_ID_PATTERN = java.util.regex.Pattern.compile("[a-z0-9_]+");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        ScriptRequest request;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            request = GSON.fromJson(reader, ScriptRequest.class);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "Invalid JSON: " + e.getMessage());
            return;
        }

        if (request == null) {
            sendError(exchange, 400, "Empty request");
            return;
        }
        if (request.scriptId() == null || request.scriptId().isBlank()) {
            sendError(exchange, 400, "Script ID is empty");
            return;
        }
        if (!SCRIPT_ID_PATTERN.matcher(request.scriptId()).matches()) {
            sendError(exchange, 400, "Script ID must use only lowercase letters, numbers and underscores");
            return;
        }

        boolean templateMode = request.templateContent() != null && !request.templateContent().isBlank();
        String script;
        if (templateMode) {
            TemplateFile template = TemplateParser.parse(request.templateContent());
            Map<String, String> values = request.values() != null ? request.values() : Map.of();
            script = TemplateScriptGenerator.generate(template, values);
        } else {
            if (request.type() == null || request.type().isBlank()) {
                sendError(exchange, 400, "Recipe type is missing");
                return;
            }
            if (request.output() == null || request.output().id() == null || request.output().id().isBlank()) {
                sendError(exchange, 400, "Output slot is empty");
                return;
            }
            script = ScriptGenerator.generate(request);
        }

        try {
            Path gameDir = PlatformHelper.getGameDir();
            Path scriptDir = gameDir.resolve("kubejs").resolve("server_scripts").resolve("generated");
            Files.createDirectories(scriptDir);

            Path out = scriptDir.resolve(request.scriptId() + ".js");
            Files.writeString(out, script, StandardCharsets.UTF_8);

            String relativePath = "kubejs/server_scripts/generated/" + request.scriptId() + ".js";
            notifyPlayers(request.scriptId());
            sendSuccess(exchange, relativePath);
        } catch (IOException e) {
            sendError(exchange, 500, "Failed to write script: " + e.getMessage());
        }
    }

    private void notifyPlayers(String scriptId) {
        MinecraftServer server = PlatformHelper.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            Component message = Component.literal(
                    "[KubeJS GUI] " + scriptId + ".js written! Run /kubejs reload to apply.");
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
            }
        });
    }

    private void sendSuccess(HttpExchange exchange, String path) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("path", path);
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", false);
        json.addProperty("error", message);
        ScriptBuilderServer.sendText(exchange, status, "application/json", GSON.toJson(json));
    }
}
