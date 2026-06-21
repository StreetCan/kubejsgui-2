package com.streetkube.kubejsgui.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.streetkube.kubejsgui.server.ScriptBuilderServer;

/**
 * GET / - serves the self-contained web UI from the mod's classpath resources.
 */
public final class WebResourceHandler implements HttpHandler {

    private static final String RESOURCE_PATH = "/assets/streetskubegui/web/index.html";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (InputStream stream = WebResourceHandler.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                ScriptBuilderServer.sendText(exchange, 404, "text/plain", "index.html not found");
                return;
            }

            byte[] bytes = stream.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
