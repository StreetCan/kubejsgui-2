package com.streetkube.kubejsgui.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.streetkube.kubejsgui.template.TemplateScanner;
import com.streetkube.kubejsgui.template.TemplateStorage;

/**
 * Template storage endpoints:
 * <ul>
 *   <li>GET    /templates              - full merged folder tree</li>
 *   <li>GET    /templates/file?path=   - raw content of one template (absolute path)</li>
 *   <li>POST   /templates/save?path=&amp;location=&amp;overwrite= - write raw body to disk</li>
 *   <li>DELETE /templates/file?path=   - delete one template (absolute path)</li>
 *   <li>POST   /templates/folder?path=&amp;location= - create a folder</li>
 * </ul>
 *
 * <p>File content for saves is sent as the raw request body (text/plain) to avoid JSON
 * escaping the multiline script block; path/location/overwrite travel as query params.
 */
public final class TemplatesHandler implements HttpHandler {

    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/templates") && method.equals("GET")) {
                handleTree(exchange);
            } else if (path.equals("/templates/file") && method.equals("GET")) {
                handleReadFile(exchange);
            } else if (path.equals("/templates/file") && method.equals("DELETE")) {
                handleDeleteFile(exchange);
            } else if (path.equals("/templates/save") && method.equals("POST")) {
                handleSave(exchange);
            } else if (path.equals("/templates/folder") && method.equals("POST")) {
                handleFolder(exchange);
            } else {
                error(exchange, 404, "not found");
            }
        } catch (IOException e) {
            error(exchange, 500, "I/O error: " + e.getMessage());
        }
    }

    private void handleTree(HttpExchange exchange) throws IOException {
        TemplateScanner.TreeResult result = TemplateScanner.scan();
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(result));
    }

    private void handleReadFile(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        Path file = TemplateStorage.validateAbsolute(query.get("path"));
        if (file == null || !Files.isRegularFile(file)) {
            error(exchange, 404, "template not found");
            return;
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        ScriptBuilderServer.sendText(exchange, 200, "text/plain", content);
    }

    private void handleDeleteFile(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        Path file = TemplateStorage.validateAbsolute(query.get("path"));
        if (file == null || !Files.isRegularFile(file)) {
            error(exchange, 404, "template not found");
            return;
        }
        Files.delete(file);
        success(exchange, file);
    }

    private void handleSave(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        String location = query.getOrDefault("location", "instance");
        boolean overwrite = "true".equalsIgnoreCase(query.get("overwrite"));

        Path root = TemplateStorage.rootFor(location);
        Path target = TemplateStorage.resolveSafe(root, query.get("path"));
        if (target == null) {
            error(exchange, 400, "invalid path");
            return;
        }
        if (Files.exists(target) && !overwrite) {
            JsonObject json = new JsonObject();
            json.addProperty("success", false);
            json.addProperty("error", "exists");
            ScriptBuilderServer.sendText(exchange, 409, "application/json", GSON.toJson(json));
            return;
        }

        String content = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
        success(exchange, target);
    }

    private void handleFolder(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        String location = query.getOrDefault("location", "instance");
        Path root = TemplateStorage.rootFor(location);
        Path target = TemplateStorage.resolveSafe(root, query.get("path"));
        if (target == null) {
            error(exchange, 400, "invalid path");
            return;
        }
        Files.createDirectories(target);
        success(exchange, target);
    }

    private void success(HttpExchange exchange, Path path) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("path", path.toAbsolutePath().toString().replace('\\', '/'));
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
