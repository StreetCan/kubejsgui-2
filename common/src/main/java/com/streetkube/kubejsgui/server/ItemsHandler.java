package com.streetkube.kubejsgui.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

import com.streetkube.kubejsgui.block.TaggedChestState;

/**
 * GET /items - returns the last-known contents of the tagged chest as a JSON array.
 */
public final class ItemsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ScriptBuilderServer.sendText(exchange, 200, "application/json", TaggedChestState.getInventoryJson());
    }
}
