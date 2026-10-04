package com.taskflow;

import com.taskflow.cli.TaskCli;
import com.taskflow.server.TaskServer;
import com.taskflow.service.TaskService;
import com.taskflow.test.TaskServiceTest;

import java.io.IOException;

/**
 * Main application entry point for TaskFlow.
 */
public class Main {
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        boolean serverOnly = false;
        boolean runTests = false;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
                }
            } else if ("--server-only".equals(args[i]) || "-s".equals(args[i])) {
                serverOnly = true;
            } else if ("--test".equals(args[i]) || "-t".equals(args[i])) {
                runTests = true;
            }
        }

        if (runTests) {
            TaskServiceTest.runAllTests();
            return;
        }

        TaskService service = new TaskService();
        TaskServer server = new TaskServer(port, service);

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("❌ Failed to start HTTP server on port " + port + ": " + e.getMessage());
            System.err.println("Continuing in CLI-only mode...");
        }

        // Clean shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down TaskFlow server...");
            server.stop();
        }));

        if (serverOnly) {
            System.out.println("⚡ TaskFlow server running in headless mode at http://localhost:" + port);
            System.out.println("Press Ctrl+C to terminate.");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
        } else {
            TaskCli cli = new TaskCli(service, port);
            cli.run();
            server.stop();
        }
    }
}
