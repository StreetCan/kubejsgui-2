package com.streetkube.kubejsgui.fluid;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

import com.streetkube.kubejsgui.server.ScriptBuilderServer;

/**
 * GET /fluids - all fluid ids in the instance ({@code minecraft:empty} excluded).
 * Response: {@code { "fluids": [ ... ] }}.
 */
public final class FluidHandler implements HttpHandler {

    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JsonObject root = new JsonObject();
        root.add("fluids", GSON.toJsonTree(FluidScanner.getFluidIds()));
        ScriptBuilderServer.sendText(exchange, 200, "application/json", GSON.toJson(root));
    }
}
