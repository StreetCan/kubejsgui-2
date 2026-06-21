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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.streetkube.kubejsgui.platform.PlatformHelper;

/**
 * Script Browser endpoints, scoped to the {@code <gameDir>/kubejs} directory:
 * <ul>
 *   <li>GET    /scripts/tree            - folder tree of all *.js files</li>
 *   <li>GET    /scripts/file?path=      - raw content of one script</li>
 *   <li>POST   /scripts/save?path=      - overwrite a script with the raw body</li>
 *   <li>DELETE /scripts/file?path=      - delete a script</li>
 *   <li>POST   /scripts/rename?path=&amp;newName= - rename a script in place</li>
 * </ul>
 *
 * <p>All file paths are validated to live under the kubejs directory (no traversal).
 */
public final class ScriptBrowserHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final java.util.regex.Pattern NAME_PATTERN =
            java.util.regex.Pattern.compile("[A-Za-z0-9_\\-. ]+");

    private static Path root() {
        return PlatformHelper.getGameDir().resolve("kubejs");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if (path.equals("/scripts/tree") && method.equals("GET")) {
                handleTree(exchange);
            } else if (path.equals("/scripts/file") && method.equals("GET")) {
                handleRead(exchange);
            } else if (path.equals("/scripts/file") && method.equals("DELETE")) {
                handleDelete(exchange);
            } else if (path.equals("/scripts/save") && method.equals("POST")) {
                handleSave(exchange);
            } else if (path.equals("/scripts/rename") && method.equals("POST")) {
                handleRename(exchange);
            } else if (path.equals("/scripts/toggle") && method.equals("POST")) {
                handleToggle(exchange);
            } else {
                error(exchange, 404, "not found");
            }
        } catch (IOException e) {
            error(exchange, 500, "I/O error: " + e.getMessage());
        }
    }

    private void handleTree(HttpExchange exchange) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("tree", Files.isDirectory(root()) ? listDir(root()) : List.of());
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(result));
    }

    private void handleRead(HttpExchange exchange) throws IOException {
        Path file = validate(parseQuery(exchange).get("path"));
        if (file == null || !Files.isRegularFile(file)) {
            error(exchange, 404, "script not found");
            return;
        }
        ScriptBuilderServer.sendText(exchange, 200, "text/plain", Files.readString(file, StandardCharsets.UTF_8));
    }

    private void handleSave(HttpExchange exchange) throws IOException {
        Path file = validate(parseQuery(exchange).get("path"));
        if (file == null || !file.getFileName().toString().endsWith(".js")) {
            error(exchange, 400, "invalid path");
            return;
        }
        String content = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        success(exchange, file);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        Path file = validate(parseQuery(exchange).get("path"));
        if (file == null || !Files.isRegularFile(file)) {
            error(exchange, 404, "script not found");
            return;
        }
        Files.delete(file);
        success(exchange, file);
    }

    private void handleRename(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        Path file = validate(query.get("path"));
        if (file == null || !Files.isRegularFile(file)) {
            error(exchange, 404, "script not found");
            return;
        }
        String newName = query.get("newName");
        if (newName == null || newName.isBlank() || newName.contains("/") || newName.contains("\\")
                || newName.contains("..") || !NAME_PATTERN.matcher(newName).matches()) {
            error(exchange, 400, "invalid name");
            return;
        }
        if (!newName.endsWith(".js")) {
            newName = newName + ".js";
        }
        Path target = file.resolveSibling(newName).normalize();
        if (!target.startsWith(root().normalize())) {
            error(exchange, 400, "invalid name");
            return;
        }
        if (Files.exists(target)) {
            error(exchange, 409, "a file with that name already exists");
            return;
        }
        Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        success(exchange, target);
    }

    private void handleToggle(HttpExchange exchange) throws IOException {
        Path file = validate(parseQuery(exchange).get("path"));
        if (file == null || !Files.isRegularFile(file)) {
            error(exchange, 404, "script not found");
            return;
        }
        String name = file.getFileName().toString();
        Path target;
        if (name.endsWith(".disabled")) {
            target = file.resolveSibling(name.substring(0, name.length() - ".disabled".length()));
        } else {
            target = file.resolveSibling(name + ".disabled");
        }
        target = target.normalize();
        if (!target.startsWith(root().normalize())) {
            error(exchange, 400, "invalid path");
            return;
        }
        if (Files.exists(target)) {
            error(exchange, 409, "a file with that name already exists");
            return;
        }
        Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        success(exchange, target);
    }

    /* ---- tree building ---- */

    private List<Node> listDir(Path dir) {
        List<Node> out = new ArrayList<>();
        List<Path> entries;
        try (Stream<Path> s = Files.list(dir)) {
            entries = s.sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return out;
        }
        for (Path p : entries) {
            if (Files.isDirectory(p)) {
                List<Node> kids = listDir(p);
                if (!kids.isEmpty()) {
                    Node n = new Node();
                    n.type = "folder";
                    n.name = p.getFileName().toString();
                    n.children = kids;
                    out.add(n);
                }
            }
        }
        for (Path p : entries) {
            String fn = p.getFileName().toString();
            if (Files.isRegularFile(p) && (fn.endsWith(".js") || fn.endsWith(".js.disabled"))) {
                Node n = new Node();
                n.type = "file";
                n.name = p.getFileName().toString();
                n.path = p.toAbsolutePath().toString().replace('\\', '/');
                out.add(n);
            }
        }
        return out;
    }

    /* ---- helpers ---- */

    private static Path validate(String abs) {
        if (abs == null || abs.isBlank()) {
            return null;
        }
        Path p;
        try {
            p = Path.of(abs).normalize();
        } catch (RuntimeException e) {
            return null;
        }
        return p.startsWith(root().normalize()) ? p : null;
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

    private static final class Node {
        String type;
        String name;
        String path;
        List<Node> children;
    }
}
