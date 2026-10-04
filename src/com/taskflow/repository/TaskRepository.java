package com.taskflow.repository;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe file-backed repository for Task entities.
 */
public class TaskRepository {
    private final Path storagePath;
    private final List<Task> tasks = new ArrayList<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public TaskRepository() {
        this(Paths.get("data", "tasks.json"));
    }

    public TaskRepository(Path storagePath) {
        this.storagePath = storagePath;
        initializeStorage();
    }

    private void initializeStorage() {
        lock.writeLock().lock();
        try {
            if (Files.exists(storagePath)) {
                loadFromFile();
            } else {
                seedInitialData();
                saveToFile();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void seedInitialData() {
        tasks.clear();
        tasks.add(Task.create(1, "Explore Java 21 Features", "Test Virtual Threads, Pattern Matching, and Records in TaskFlow.", Priority.URGENT));
        tasks.add(Task.create(2, "Test Web Dashboard", "Open http://localhost:8080 to test the embedded web interface.", Priority.HIGH));
        tasks.add(Task.create(3, "Try CLI Commands", "Add, toggle, and manage tasks from the terminal prompt.", Priority.MEDIUM));
        tasks.add(Task.create(4, "Build Executable JAR", "Run build.bat to package the application into a standalone JAR.", Priority.LOW));
        idSequence.set(5);
    }

    private void loadFromFile() {
        try {
            String content = Files.readString(storagePath, StandardCharsets.UTF_8);
            List<Task> loaded = JsonUtil.parseTaskList(content);
            tasks.clear();
            tasks.addAll(loaded);

            int maxId = tasks.stream().mapToInt(Task::id).max().orElse(0);
            idSequence.set(maxId + 1);
        } catch (IOException e) {
            System.err.println("Warning: Failed to load tasks from " + storagePath + ": " + e.getMessage());
            seedInitialData();
        }
    }

    private void saveToFile() {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            String json = JsonUtil.taskListToJson(tasks);
            Files.writeString(storagePath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Warning: Failed to save tasks to " + storagePath + ": " + e.getMessage());
        }
    }

    public List<Task> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(tasks);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Task> findById(int id) {
        lock.readLock().lock();
        try {
            return tasks.stream().filter(t -> t.id() == id).findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Task save(String title, String description, Priority priority) {
        lock.writeLock().lock();
        try {
            int newId = idSequence.getAndIncrement();
            Task task = Task.create(newId, title, description, priority);
            tasks.add(task);
            saveToFile();
            return task;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean update(Task updated) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).id() == updated.id()) {
                    tasks.set(i, updated);
                    saveToFile();
                    return true;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean deleteById(int id) {
        lock.writeLock().lock();
        try {
            boolean removed = tasks.removeIf(t -> t.id() == id);
            if (removed) {
                saveToFile();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
