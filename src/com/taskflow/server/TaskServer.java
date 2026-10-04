package com.taskflow.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.service.TaskService;
import com.taskflow.util.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP Server for TaskFlow powered by Java 21 Virtual Threads.
 */
public class TaskServer {
    private final int port;
    private final TaskService service;
    private HttpServer server;

    public TaskServer(int port, TaskService service) {
        this.port = port;
        this.service = service;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Java 21 Virtual Threads for high concurrency
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/api/tasks", new TasksHandler());
        server.createContext("/api/tasks/toggle", new ToggleHandler());
        server.createContext("/api/tasks/delete", new DeleteHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/", new StaticFileHandler());

        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    public int getPort() {
        return port;
    }

    private class TasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
                    String q = queryParams.get("search");
                    String pStr = queryParams.get("priority");
                    String cStr = queryParams.get("completed");

                    Priority priority = (pStr != null && !pStr.isEmpty()) ? Priority.fromString(pStr) : null;
                    Boolean completed = (cStr != null && !cStr.isEmpty()) ? Boolean.parseBoolean(cStr) : null;

                    List<Task> result = service.searchTasks(q, priority, completed);
                    String json = JsonUtil.taskListToJson(result);
                    sendResponse(exchange, 200, json, "application/json; charset=UTF-8");
                } else if ("POST".equals(method)) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String title = JsonUtil.extractString(body, "title");
                    String desc = JsonUtil.extractString(body, "description");
                    String pStr = JsonUtil.extractString(body, "priority");

                    if (title == null || title.trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\":\"Title is required\"}", "application/json; charset=UTF-8");
                        return;
                    }

                    Task created = service.addTask(title, desc, Priority.fromString(pStr));
                    String json = JsonUtil.taskToJson(created);
                    sendResponse(exchange, 201, json, "application/json; charset=UTF-8");
                } else {
                    sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json; charset=UTF-8");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json; charset=UTF-8");
            }
        }
    }

    private class ToggleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(method) || "PUT".equals(method)) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Integer id = JsonUtil.extractInt(body, "id");
                    if (id == null) {
                        Map<String, String> params = parseQueryParams(exchange.getRequestURI());
                        if (params.containsKey("id")) {
                            id = Integer.parseInt(params.get("id"));
                        }
                    }

                    if (id != null && service.toggleTask(id)) {
                        sendResponse(exchange, 200, "{\"success\":true,\"id\":" + id + "}", "application/json; charset=UTF-8");
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Task not found\"}", "application/json; charset=UTF-8");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json; charset=UTF-8");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json; charset=UTF-8");
            }
        }
    }

    private class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(method) || "DELETE".equals(method)) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Integer id = JsonUtil.extractInt(body, "id");
                    if (id == null) {
                        Map<String, String> params = parseQueryParams(exchange.getRequestURI());
                        if (params.containsKey("id")) {
                            id = Integer.parseInt(params.get("id"));
                        }
                    }

                    if (id != null && service.deleteTask(id)) {
                        sendResponse(exchange, 200, "{\"success\":true,\"id\":" + id + "}", "application/json; charset=UTF-8");
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Task not found\"}", "application/json; charset=UTF-8");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}", "application/json; charset=UTF-8");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json; charset=UTF-8");
            }
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod().toUpperCase())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            Map<String, Object> stats = service.getStatistics();
            sendResponse(exchange, 200, JsonUtil.mapToJson(stats), "application/json; charset=UTF-8");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr == null || pathStr.equals("/") || pathStr.isEmpty()) {
                pathStr = "index.html";
            } else if (pathStr.startsWith("/")) {
                pathStr = pathStr.substring(1);
            }

            Path staticFile = Paths.get("static", pathStr);
            if (Files.exists(staticFile) && !Files.isDirectory(staticFile)) {
                String contentType = getMimeType(pathStr);
                byte[] bytes = Files.readAllBytes(staticFile);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Fallback HTML page if static directory is missing
                String fallbackHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>TaskFlow</title></head>
                    <body style="font-family:sans-serif; text-align:center; padding:50px;">
                        <h1>⚡ TaskFlow API is running!</h1>
                        <p>Access the REST API at <a href="/api/tasks">/api/tasks</a> or <a href="/api/stats">/api/stats</a></p>
                    </body>
                    </html>
                    """;
                sendResponse(exchange, 200, fallbackHtml, "text/html; charset=UTF-8");
            }
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> queryPairs = new HashMap<>();
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return queryPairs;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                queryPairs.put(key, value);
            }
        }
        return queryPairs;
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
