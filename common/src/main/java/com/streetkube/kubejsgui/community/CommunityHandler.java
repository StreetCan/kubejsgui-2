package com.streetkube.kubejsgui.community;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.streetkube.kubejsgui.server.ScriptBuilderServer;

/**
 * HTTP endpoints for the Community tab. Mirrors the same backend used by the (future)
 * in-game {@code /kubejsgui repo} commands via {@link RepoConfig} + {@link CommunityCache}.
 */
public final class CommunityHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if (path.equals("/community/entries") && method.equals("GET")) {
                handleEntries(exchange);
            } else if (path.equals("/community/repos") && method.equals("GET")) {
                handleRepos(exchange);
            } else if (path.equals("/community/file") && method.equals("GET")) {
                handleFile(exchange);
            } else if (path.equals("/community/repos/add") && method.equals("POST")) {
                handleAdd(exchange);
            } else if (path.equals("/community/repos/remove") && method.equals("POST")) {
                handleRemove(exchange);
            } else if (path.equals("/community/repos/toggle") && method.equals("POST")) {
                handleToggle(exchange);
            } else if (path.equals("/community/repos/refresh") && method.equals("POST")) {
                handleRefresh(exchange);
            } else if (path.equals("/community/install") && method.equals("POST")) {
                handleInstall(exchange);
            } else {
                error(exchange, 404, "not found");
            }
        } catch (IOException e) {
            error(exchange, 500, "I/O error: " + e.getMessage());
        }
    }

    private void handleEntries(HttpExchange exchange) throws IOException {
        CommunityCache.ensureLoaded();
        JsonObject root = new JsonObject();
        root.add("entries", GSON.toJsonTree(CommunityCache.getEntries()));
        JsonArray errors = new JsonArray();
        for (CommunityCache.RepoStatus s : CommunityCache.getErrors()) {
            JsonObject e = new JsonObject();
            e.addProperty("url", s.url);
            e.addProperty("error", s.error);
            errors.add(e);
        }
        root.add("repoErrors", errors);
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(root));
    }

    private void handleRepos(HttpExchange exchange) throws IOException {
        CommunityCache.ensureLoaded();
        JsonObject root = new JsonObject();
        root.add("repos", GSON.toJsonTree(CommunityCache.getStatuses()));
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(root));
    }

    private void handleFile(HttpExchange exchange) throws IOException {
        String url = parseQuery(exchange).get("url");
        if (url == null || !url.toLowerCase().startsWith("https://")) {
            error(exchange, 400, "Only https:// URLs are allowed");
            return;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (RuntimeException e) {
            error(exchange, 400, "Invalid URL");
            return;
        }
        if (uri.getHost() == null || !CommunityCache.activeHosts().contains(uri.getHost())) {
            error(exchange, 400, "URL domain does not match any configured repo");
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                error(exchange, 502, "HTTP " + response.statusCode());
                return;
            }
            String body = response.body();
            if (body.length() > 500_000) {
                error(exchange, 413, "File too large");
                return;
            }
            ScriptBuilderServer.sendText(exchange, 200, "text/plain", body);
        } catch (Exception e) {
            error(exchange, 502, "Failed to fetch: " + e.getMessage());
        }
    }

    private void handleAdd(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String url = RepoFetcher.normalizeRepoUrl(optString(body, "url"));
        if (url == null || !url.toLowerCase().startsWith("https://")) {
            error(exchange, 400, "Repo URL must start with https://");
            return;
        }
        RepoFetcher.Result result;
        try {
            result = RepoFetcher.fetchAsync(url).get();
        } catch (Exception e) {
            result = RepoFetcher.Result.error(e.getMessage());
        }
        if (result.error != null) {
            error(exchange, 400, "Could not validate repo: " + result.error);
            return;
        }
        boolean added = RepoConfig.add(url);
        CommunityCache.refresh();
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("added", added);
        json.addProperty("entryCount", result.index.entries.size());
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
    }

    private void handleRemove(HttpExchange exchange) throws IOException {
        String url = optString(readJson(exchange), "url");
        if (url == null) {
            error(exchange, 400, "Missing url");
            return;
        }
        RepoConfig.remove(url);
        CommunityCache.refresh();
        ok(exchange);
    }

    private void handleToggle(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String url = optString(body, "url");
        if (url == null) {
            error(exchange, 400, "Missing url");
            return;
        }
        boolean enabled = body.has("enabled") && body.get("enabled").getAsBoolean();
        RepoConfig.setEnabled(url, enabled);
        CommunityCache.refresh();
        ok(exchange);
    }

    private void handleRefresh(HttpExchange exchange) throws IOException {
        CommunityCache.refresh();
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("entries", CommunityCache.getEntries().size());
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(json));
    }

    private void handleInstall(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        CommunityInstaller.Result result = CommunityInstaller.install(
                optString(body, "fileUrl"),
                optString(body, "expectedType"),
                optString(body, "expectedName"));
        ScriptBuilderServer.sendText(exchange, result.success ? 200 : 400,
                "application/json", GSON.toJson(result));
    }

    /* ---- helpers ---- */

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
