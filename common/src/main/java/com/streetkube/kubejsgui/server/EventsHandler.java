package com.streetkube.kubejsgui.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.streetkube.kubejsgui.block.TaggedChestState;

/**
 * GET /events - an SSE stream that pushes an "inventory_update" event each time the
 * tagged chest's contents change.
 */
public final class EventsHandler implements HttpHandler {

    private static final List<HttpExchange> CLIENTS = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.getResponseHeaders().add("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);

        CLIENTS.add(exchange);

        if (!sendEvent(exchange, TaggedChestState.getInventoryJson())) {
            CLIENTS.remove(exchange);
        }
    }

    public static void broadcastInventoryUpdate(String json) {
        synchronized (CLIENTS) {
            CLIENTS.removeIf(exchange -> !sendEvent(exchange, json));
        }
    }

    private static boolean sendEvent(HttpExchange exchange, String json) {
        try {
            String payload = "data: " + json + "\n\n";
            OutputStream os = exchange.getResponseBody();
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (IOException e) {
            try {
                exchange.close();
            } catch (Exception ignored) {
                // already closed
            }
            return false;
        }
    }
}
