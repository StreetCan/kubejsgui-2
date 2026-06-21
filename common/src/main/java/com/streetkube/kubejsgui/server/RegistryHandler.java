package com.streetkube.kubejsgui.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

import com.streetkube.kubejsgui.recipe.RegistryScanner;

/**
 * Read-only registry endpoints powering the Modify tab's search/autocomplete fields:
 * <ul>
 *   <li>GET /items/registry    - every item id (with display/durability/stack hints)</li>
 *   <li>GET /blocks/registry   - every block id</li>
 *   <li>GET /entities/registry - every entity-type id (player excluded)</li>
 * </ul>
 *
 * <p>Distinct from {@code GET /items}, which returns the tagged-chest inventory.
 */
public final class RegistryHandler implements HttpHandler {

    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Object payload;
        switch (path) {
            case "/items/registry" -> payload = RegistryScanner.getItems();
            case "/blocks/registry" -> payload = RegistryScanner.getBlocks();
            case "/entities/registry" -> payload = RegistryScanner.getEntities();
            default -> {
                ScriptBuilderServer.sendText(exchange, 404, "application/json", "{\"error\":\"not found\"}");
                return;
            }
        }
        if (payload == null) {
            payload = List.of();
        }
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(payload));
    }
}
