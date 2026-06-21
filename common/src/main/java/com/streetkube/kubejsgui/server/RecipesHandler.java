package com.streetkube.kubejsgui.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.streetkube.kubejsgui.platform.PlatformHelper;
import com.streetkube.kubejsgui.recipe.RecipeSchema;
import com.streetkube.kubejsgui.recipe.RecipeSchemas;
import com.streetkube.kubejsgui.recipe.RecipeScanner;

/**
 * GET /recipes/types and GET /recipes/schema/:type.
 */
public final class RecipesHandler implements HttpHandler {

    private static final Gson GSON = new Gson();
    private static final String TYPES_PATH = "/recipes/types";
    private static final String ALL_PATH = "/recipes/all";
    private static final String SCHEMA_PREFIX = "/recipes/schema/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals(TYPES_PATH)) {
            handleTypes(exchange);
        } else if (path.equals(ALL_PATH)) {
            handleAll(exchange);
        } else if (path.startsWith(SCHEMA_PREFIX) && path.length() > SCHEMA_PREFIX.length()) {
            handleSchema(exchange, path.substring(SCHEMA_PREFIX.length()));
        } else {
            ScriptBuilderServer.sendText(exchange, 404, "application/json", "{\"error\":\"not found\"}");
        }
    }

    private void handleTypes(HttpExchange exchange) throws IOException {
        MinecraftServer server = PlatformHelper.getServer();
        Set<String> types = server != null ? RecipeScanner.getLoadedRecipeTypes(server) : Set.of();
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(types));
    }

    private void handleAll(HttpExchange exchange) throws IOException {
        MinecraftServer server = PlatformHelper.getServer();
        var recipes = server != null ? RecipeScanner.getAllRecipes(server) : java.util.List.of();
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(recipes));
    }

    private void handleSchema(HttpExchange exchange, String rawType) throws IOException {
        String typeId = URLDecoder.decode(rawType, StandardCharsets.UTF_8);
        RecipeSchema schema = RecipeSchemas.get(typeId);
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(schema));
    }
}
