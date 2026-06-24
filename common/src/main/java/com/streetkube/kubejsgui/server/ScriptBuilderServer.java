package com.streetkube.kubejsgui.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.streetkube.kubejsgui.web.WebResourceHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the lifecycle of the local-only HTTP server backing the script builder web UI.
 */
public final class ScriptBuilderServer {

    private static final Logger LOGGER = Logger.getLogger("streetskubegui");
    private static final String BIND_ADDRESS = "127.0.0.1";
    private static final int FIRST_PORT = 7890;
    private static final int LAST_PORT = 7899;

    private static HttpServer server;
    private static ExecutorService executor;
    private static int boundPort = -1;

    private ScriptBuilderServer() {
    }

    /**
     * Starts the HTTP server if it isn't already running. Safe to call repeatedly -
     * if already running this just returns without restarting.
     */
    public static synchronized void ensureStarted() {
        if (server != null) {
            return;
        }

        for (int port = FIRST_PORT; port <= LAST_PORT; port++) {
            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress(BIND_ADDRESS, port), 0);
                executor = Executors.newFixedThreadPool(4);
                httpServer.setExecutor(executor);

                registerHandlers(httpServer);

                httpServer.start();
                server = httpServer;
                boundPort = port;
                LOGGER.info("[KubeJS GUI] HTTP server listening on http://" + BIND_ADDRESS + ":" + port);
                // Warm the community cache in the background so entries are ready when the UI opens.
                com.streetkube.kubejsgui.community.CommunityCache.refreshAsync();
                return;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "[KubeJS GUI] Port " + port + " unavailable", e);
            }
        }

        LOGGER.severe("[KubeJS GUI] Failed to bind HTTP server on any port in range "
                + FIRST_PORT + "-" + LAST_PORT);
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            boundPort = -1;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public static synchronized boolean isRunning() {
        return server != null;
    }

    public static synchronized int getPort() {
        return boundPort;
    }

    private static void registerHandlers(HttpServer httpServer) {
        httpServer.createContext("/", withCors(new WebResourceHandler()));
        httpServer.createContext("/items", withCors(new ItemsHandler()));
        httpServer.createContext("/events", withCors(new EventsHandler()));
        httpServer.createContext("/recipes", withCors(new RecipesHandler()));
        httpServer.createContext("/script", withCors(new ScriptHandler()));
        httpServer.createContext("/templates", withCors(new TemplatesHandler()));
        httpServer.createContext("/scripts", withCors(new ScriptBrowserHandler()));
        httpServer.createContext("/itembuilder", withCors(new ItemBuilderHandler()));
        httpServer.createContext("/blockbuilder", withCors(new BlockBuilderHandler()));
        httpServer.createContext("/community", withCors(new com.streetkube.kubejsgui.community.CommunityHandler()));
        httpServer.createContext("/rawscript", withCors(new RawScriptHandler()));

        // Modify tab: recipe/item/loot modification + supporting registry data sources.
        // (/recipes/all is already served by the /recipes context above.) More specific
        // contexts win prefix-matching, so /items/registry routes here, not to ItemsHandler.
        RegistryHandler registryHandler = new RegistryHandler();
        httpServer.createContext("/items/registry", withCors(registryHandler));
        httpServer.createContext("/blocks/registry", withCors(registryHandler));
        httpServer.createContext("/entities/registry", withCors(registryHandler));
        com.streetkube.kubejsgui.modify.ModifyHandler modifyHandler =
                new com.streetkube.kubejsgui.modify.ModifyHandler();
        httpServer.createContext("/modify", withCors(modifyHandler));
        httpServer.createContext("/loottable", withCors(modifyHandler));

        // Fluid picker + Tags tab.
        httpServer.createContext("/fluids", withCors(new com.streetkube.kubejsgui.fluid.FluidHandler()));
        httpServer.createContext("/tags", withCors(new com.streetkube.kubejsgui.tags.TagHandler()));

        // Settings (custom universal templates folder, etc.).
        httpServer.createContext("/config", withCors(new ConfigHandler()));
    }

    /**
     * Wraps a handler with the CORS headers required by every response and handles
     * the OPTIONS preflight request directly.
     */
    public static HttpHandler withCors(HttpHandler handler) {
        return exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            handler.handle(exchange);
        };
    }

    public static void sendText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends the in-game chat message advertising the running web UI, with a clickable
     * link that opens it in the player's browser.
     */
    public static void sendReadyMessage(ServerPlayer player) {
        if (boundPort < 0) {
            return;
        }

        String url = "http://" + BIND_ADDRESS + ":" + boundPort;
        Component link = Component.literal(url)
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));

        player.sendSystemMessage(Component.literal("[KubeJS GUI] Script builder ready → ").append(link));
    }
}
