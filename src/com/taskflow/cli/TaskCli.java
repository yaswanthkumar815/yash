package com.taskflow.cli;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.service.TaskService;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive Command Line Interface for TaskFlow.
 */
public class TaskCli {
    private final TaskService service;
    private final int serverPort;
    private final Scanner scanner;

    public TaskCli(TaskService service, int serverPort) {
        this.service = service;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("\n👉 Select an option (1-8): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> listTasks(service.getAllTasks(), "All Tasks");
                case "2" -> listTasks(service.searchTasks(null, null, false), "Pending Tasks");
                case "3" -> handleAddTask();
                case "4" -> handleToggleTask();
                case "5" -> handleDeleteTask();
                case "6" -> handleShowStats();
                case "7" -> handleOpenBrowser();
                case "8", "exit", "quit" -> {
                    System.out.println("\n👋 Thank you for using TaskFlow. Goodbye!");
                    running = false;
                }
                default -> System.out.println("❌ Invalid option. Please choose between 1 and 8.");
            }
        }
    }

    private void printBanner() {
        System.out.println("""
            ======================================================================
              ⚡  TASKFLOW - Java 21 Task Management & Productivity Hub
            ======================================================================
              🌐 Embedded Web Dashboard: http://localhost:%d
              🚀 Features: Virtual Threads, Immutable Records, File Persistence
            ======================================================================
            """.formatted(serverPort));
    }

    private void printMenu() {
        System.out.println("""
            ----------------------------------------------------------------------
              1. 📋 View All Tasks
              2. ⏳ View Pending Tasks
              3. ➕ Add New Task
              4. 🔄 Toggle Task Status (Complete / Uncomplete)
              5. 🗑️ Delete Task
              6. 📊 View Statistics Dashboard
              7. 🌐 Open Web Dashboard in Browser
              8. 🚪 Exit
            ----------------------------------------------------------------------""");
    }

    private void listTasks(List<Task> tasks, String heading) {
        System.out.println("\n--- " + heading + " (" + tasks.size() + ") ---");
        if (tasks.isEmpty()) {
            System.out.println("  (No tasks found)");
            return;
        }

        System.out.printf("%-4s | %-6s | %-8s | %-30s | %-16s%n", "ID", "STATUS", "PRIORITY", "TITLE", "CREATED");
        System.out.println("-".repeat(72));

        for (Task t : tasks) {
            String status = t.completed() ? "[DONE]" : "[TODO]";
            String title = t.title().length() > 28 ? t.title().substring(0, 25) + "..." : t.title();
            System.out.printf("%-4d | %-6s | %-8s | %-30s | %-16s%n",
                    t.id(), status, t.priority().name(), title, t.createdAt());
            if (t.description() != null && !t.description().isEmpty()) {
                System.out.println("     └─ Description: " + t.description());
            }
        }
    }

    private void handleAddTask() {
        System.out.println("\n--- Add New Task ---");
        System.out.print("Enter task title: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("❌ Title cannot be empty.");
            return;
        }

        System.out.print("Enter description (optional, press Enter to skip): ");
        String desc = scanner.nextLine().trim();

        System.out.println("Select Priority: 1) LOW  2) MEDIUM  3) HIGH  4) URGENT (Default: 2)");
        System.out.print("Enter choice (1-4): ");
        String pChoice = scanner.nextLine().trim();
        Priority priority = switch (pChoice) {
            case "1" -> Priority.LOW;
            case "3" -> Priority.HIGH;
            case "4" -> Priority.URGENT;
            default -> Priority.MEDIUM;
        };

        Task created = service.addTask(title, desc, priority);
        System.out.println("✅ Task created successfully with ID: " + created.id());
    }

    private void handleToggleTask() {
        System.out.print("\nEnter Task ID to toggle completion: ");
        String input = scanner.nextLine().trim();
        try {
            int id = Integer.parseInt(input);
            boolean success = service.toggleTask(id);
            if (success) {
                var task = service.getTaskById(id).orElseThrow();
                String statusStr = task.completed() ? "COMPLETED" : "PENDING";
                System.out.println("✅ Task #" + id + " status changed to: " + statusStr);
            } else {
                System.out.println("❌ Task with ID #" + id + " not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid task ID. Please enter a valid number.");
        }
    }

    private void handleDeleteTask() {
        System.out.print("\nEnter Task ID to delete: ");
        String input = scanner.nextLine().trim();
        try {
            int id = Integer.parseInt(input);
            boolean deleted = service.deleteTask(id);
            if (deleted) {
                System.out.println("✅ Task #" + id + " deleted successfully.");
            } else {
                System.out.println("❌ Task with ID #" + id + " not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid task ID.");
        }
    }

    private void handleShowStats() {
        Map<String, Object> stats = service.getStatistics();
        System.out.println("\n📊 --- Productivity Dashboard ---");
        System.out.println("  • Total Tasks:     " + stats.get("total"));
        System.out.println("  • Completed Tasks: " + stats.get("completed"));
        System.out.println("  • Pending Tasks:   " + stats.get("pending"));
        System.out.println("  • Urgent Tasks:    " + stats.get("urgent"));

        long total = (long) stats.get("total");
        long completed = (long) stats.get("completed");
        int percent = total > 0 ? (int) ((completed * 100) / total) : 0;
        int barLength = 20;
        int filled = (percent * barLength) / 100;
        String bar = "█".repeat(filled) + "░".repeat(barLength - filled);
        System.out.printf("  • Completion Rate: [%s] %d%%%n", bar, percent);
    }

    private void handleOpenBrowser() {
        String url = "http://localhost:" + serverPort;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("🌐 Opened " + url + " in your default web browser.");
            } else {
                // Fallback for Windows command
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                System.out.println("🌐 Launched browser: " + url);
            }
        } catch (Exception e) {
            System.out.println("ℹ️ Please open your browser and navigate to: " + url);
        }
    }
}
